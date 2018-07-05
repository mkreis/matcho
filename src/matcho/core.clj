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

(defn- match-recur [errors path example pattern]
  (cond
    (and (map? example)
         (map? pattern))
    (reduce (fn [errors [k v]]
              (let [path  (conj path k)
                    ev (get example k)]
                (match-recur errors path ev v)))
            errors pattern)

    (and (vector? pattern)
         (seqable? example))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev  (nth (vec example) k nil)]
                (match-recur errors path ev v)))
            errors
            (map (fn [x i] [i x]) pattern (range)))

    :else (let [err (smart-explain-data pattern example)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn match*
  "Match against each pattern"
  [example & patterns]
  (reduce (fn [acc pattern] (match-recur acc [] example pattern)) [] patterns))

(defmacro match
  "Match against each pattern and assert with is"
  [example & pattern]
  `(let [example# ~example
         patterns# [~@pattern]
         errors# (apply match* example# patterns#)]
     (if-not (empty? errors#)
       (is false (pr-str errors# example# patterns#))
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
  [example pattern]
  `(let [sp# (to-spec ~pattern)]
     (::s/problems (s/explain-data sp# ~example))))

(defmacro matcho
  "Match against one pattern and assert with is"
  [example pattern]
  `(let [sp# (to-spec ~pattern)
         res# (s/valid? sp#  ~example)
         es# (s/explain-str sp# ~example)]
     (is res# (str (pr-str ~example) "\n" es#))))

(comment

  (match* [1 3] [1 2])

  (smart-explain-data pos? -1)

  (matcho* -1 pos?)

  (matcho* [1 -2 3] [neg? neg? neg?])
  (to-spec [neg? neg? neg?])

  (matcho* [1 2] (s/coll-of keyword?))

  (to-spec (s/coll-of keyword?))

  )
