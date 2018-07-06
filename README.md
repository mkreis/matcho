# matcho [![CircleCI](https://circleci.com/gh/HealthSamurai/matcho.svg?style=shield)](https://circleci.com/gh/HealthSamurai/matcho) [![Join gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/healthsamurai/matcho)

[![Clojars Project](http://clojars.org/healthsamurai/matcho/latest-version.svg)](http://clojars.org/healthsamurai/matcho)

## Idea

The main goal is to provide the simpliest DSL to describe pattern of expected
value.

### Problem

Not so easy to write tests with multiple asserts. Code grows fast, a lot of
repetitions, hard to read.

```clj
(def person
  {:age      42
   :name     "Health Samurai"
   :email    "samurai@hs.io"
   :favorite {:numbers [1 3 17]}})

(is (even? (get person :age)))
(is (re-find #"Health.*" (get person :name)))
(is (= [1 3 17] (get-in person [:favorite :numbers])))
```

### Solution

Part of expected datastructure can be used as a pattern. If exact value not know
a predicate or spec can be used instead of it. `matcho/match` will check if
expected value matches the pattern(s) and assert with `is`.

```clj
(def person-pattern
  {:age      #(even? %)
   :name     #"Health.*"
   :favorite {:numbers [1 3 17]}})
   
(match person person-pattern)
```

## Usage

Add following project dependency to deps.edn:

```clj
{healthsamurai/matcho {:mvn/version "RELEASE"}}
```

### Snippets

Understand and pick out needed parts:

```clj
(ns hello-world.core
  (:require [matcho.core :refer [match match* matcho matcho*]]))
  
(match* 1 )

(testing "Matches"
  (match 1 1)
  (match [1] [1])
  (match  {:a 1 :b 2} {:a 1})
  (match  {:a 1 :b 2} {:a odd?})
  (match {:a 2} {:a pos?})
  (match  {:a [1 2 3]} {:a #(= 3 (count %))})

  (match {:a {:b [{:c 1 :x 5} {:c 2 :x 6}]}}
          {:a {:b [{:c 1} {:c 2}]}}))

(testing "Errors"

  (is (= [{:path [:a],
            :expected "#function[matcho.core-test/count-4?]"
            :but [1 2 3]}]
          (match*  {:a [1 2 3]} {:a count-4?})))
  (is (= [] (match* {} [])))

  (is (= (match* {:a 2} {:a 1})
          [{:path [:a], :expected "1", :but 2}] ))

  (is (= (match* {:a 2} {:a neg?})
          [{:path [:a], :expected "#function[clojure.core/neg?]", :but 2}]))

  (is (= (match* {} {:x ""})
          [{:path [:x], :expected "\"\"", :but nil}] ))

  (is (= (match* {:a [1]} {:a [2]})
          [{:path [:a 0], :expected "2", :but 1}]))

  (is (= (match* {:a {:b "baaa"}}
                  {:a {:b #"^a"}} )
          [{:path [:a :b], :expected "#\"^a\"", :but "baaa"}]))

  (is (= (match* {:a [1 {:c 3}]} {:a [1 {:c 4}]})
          [{:path [:a 1 :c], :expected "4", :but 3}])))
          
          
(s/def ::pos-coll (s/coll-of pos?))
(match (match*  {:a [1 -2 3]} {:a ::pos-coll})
        [{:path [:a]
          :expected "confirms to spec :matcho.core-test/pos-coll"
          :but "In: [1] val: -2 fails spec: :matcho.core-test/pos-coll predicate: pos?\n"}])
```

## License

Copyright © 2016 HealthSamurai

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
