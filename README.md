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
  (:require [clojure.test :refer :all]
            [matcho.core :as :m]))
  
(deftest readme-test
  (is (m/valid? pos? 1))
  (m/assert 1 1)
  (m/assert {:status #(< % 300)
             :body   #(not (empty? %))}
            {:status 200
             :body   "hello"})
  (m/assert ::pos-coll [1 2 3])
  (m/assert [{:expected #"conforms.*pos-coll"}]
            (m/explain-data ::pos-coll [1 -1 2])))

```

More advanced examples can be found [here](./test/core_test.clj).

## License

Copyright © 2016 HealthSamurai

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
