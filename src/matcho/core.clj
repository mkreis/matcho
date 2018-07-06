(ns matcho.core
 (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer :all]))

(defn smart-explain-data [p x]
  (cond
    (instance? clojure.spec.alpha.Specize p)
    (when-not (s/valid? p x)
      {:expected (str "conforms to spec: " p) :but (s/explain-data p x)})

    (and (string? x) (instance? java.util.regex.Pattern p))
    (when-not (re-find p x)
      {:expected (str "match regexp: " p) :but x})

    (fn? p)
    (when-not (p x)
      {:expected (pr-str p) :but x})

    (and (keyword? p) (s/get-spec p))
    (let [sp (s/get-spec p)]
      (when-not (s/valid? p x)
        {:expected (str "conforms to spec: " p) :but (s/explain-data p x)}))

    :else (when-not (= p x)
            {:expected p :but x})))

(defn- match-recur [errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))
    (reduce (fn [errors [k v]]
              (let [path  (conj path k)
                    ev (get x k)]
                (match-recur errors path ev v)))
            errors pattern)

    (and (vector? pattern)
         (seqable? x))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev  (nth (vec x) k nil)]
                (match-recur errors path ev v)))
            errors
            (map (fn [x i] [i x]) pattern (range)))

    :else (let [err (smart-explain-data pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn match*
  "Match against each pattern"
  [x & patterns]
  (reduce (fn [acc pattern] (match-recur acc [] x pattern)) [] patterns))

(defmacro match
  "Match against each pattern and assert with is"
  [x & pattern]
  `(let [x# ~x
         patterns# [~@pattern]
         errors# (apply match* x# patterns#)]
     (if-not (empty? errors#)
       (is false (pr-str errors# x# patterns#))
       (is true))))


(defmacro to-spec
  [pattern]
  (cond
    (symbol? pattern) pattern
    (instance? clojure.lang.Cons pattern) pattern
    (list? pattern) pattern
    (instance? clojure.spec.alpha.Specize pattern)  (throw (Exception. "ups")) ;;pattern
    (fn? pattern) pattern
    (map? pattern)
    (let [nns (name (gensym "n"))
          nks (mapv #(keyword nns (name %)) (keys pattern))
          ks  (map (fn [[k v]] (list 's/def (keyword nns (name k)) (list 'to-spec v))) pattern)]
      `(do ~@ks (s/keys :req-un ~nks)))

    (vector? pattern)
    (let [nns (name (gensym "n"))
          cats (loop [i 0
                      [p & ps] pattern
                      cats []]
                 (if p
                   (recur (inc i)
                          ps
                          (conj cats (keyword nns (str "i" i)) (list 'to-spec p)))
                   cats))]
      `(s/cat ~@cats :rest (s/* (constantly true))))

    :else `(conj #{} ~pattern)))

(defmacro matcho*
  "Match against one pattern"
  [x pattern]
  `(let [sp# (to-spec ~pattern)]
     (::s/problems (s/explain-data sp# ~x))))

(defmacro matcho
  "Match against one pattern and assert with is"
  [x pattern]
  `(let [sp# (to-spec ~pattern)
         res# (s/valid? sp#  ~x)
         es# (s/explain-str sp# ~x)]
     (is res# (str (pr-str ~x) "\n" es#))))

(defn valid? [pattern x]
  (if (empty? (match* x pattern))
    true
    false))

(defn explain-data
  "Returns list of errors or nil"
  [pattern x]
  (let [errors (match* x pattern)]
    (when (not-empty errors) errors)))

(defmacro assert [pattern x]
  `(match ~x ~pattern))


(comment

  (def person
    {:age      42
     :name     "Health Samurai"
     :email    "samurai@hs.io"
     :favorite {:numbers [1 3 17]}})

  (def person-pattern
    {:age      #(even? %)
     :name     #"Health.*"
     :favorite {:numbers [1 3 17]}})

  (valid? person-pattern person)
  (valid? [1 3] [1 2])

  (smart-explain-data pos? -1)

  (matcho* -1 pos?)

  (matcho* [1 -2 3] [neg? neg? neg?])
  (to-spec [neg? neg? neg?])

  (matcho* [1 2] (s/coll-of keyword?))

  (to-spec (s/coll-of keyword?))

  )
