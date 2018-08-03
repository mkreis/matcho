(ns matcho.asserts
  (:require [matcho.core :refer :all]
            [clojure.test :refer :all]))


(defmacro match
  "Match against each pattern and assert with is"
  [x & pattern]
  `(let [x#        ~x
         patterns# [~@pattern]
         errors#   (apply match* x# patterns#)]
     (if-not (empty? errors#)
       (is false (pr-str errors# x# patterns#))
       (is true))))

(defmacro matcho
  "Match against one pattern and assert with is"
  [x pattern]
  `(let [sp#  (to-spec ~pattern)
         res# (s/valid? sp#  ~x)
         es#  (s/explain-str sp# ~x)]
     (is res# (str (pr-str ~x) "\n" es#))))

(defmacro assert [pattern x]
  `(match ~x ~pattern))
