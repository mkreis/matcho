# matcho [![CircleCI](https://circleci.com/gh/HealthSamurai/matcho.svg?style=shield)](https://circleci.com/gh/HealthSamurai/matcho) [![Join gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/healthsamurai/matcho)

[![Clojars Project](http://clojars.org/healthsamurai/matcho/latest-version.svg)](http://clojars.org/healthsamurai/matcho)

## Idea

The main goal is to provide the simpliest DSL to describe pattern of expected
value.

### Problem

Not so easy to write tests with multiple asserts. Code grows fast, a lot of
repetitions, hard to read.

```clj
(deftest general-patch-test
  (let [body (:body resp)]
    (is (< (:status resp) 300))
    (is (= (:title patch) (get-in body [:article :title])))
    (is (= (:description patch) (get-in body [:article :description])))
    (is (s/valid? ::str-coll (get-in body [:meta :tags])))))
```

### Solution

Part of expected datastructure can be used as a pattern. If exact value not
known a predicate or spec can be used instead of it. `matcho/assert` will check
if expected value matches the pattern(s) and internally assert with `is`.

```clj
(deftest matcho-patch-test
  (def pattern
    {:status #(< % 300)
     :body   {:article patch
              :meta    {:tags ::str-coll}}})
  (m/assert pattern resp))
```

Full example can be found [here](./test/matcho/core_test.clj).

## Usage

### Deps
Add following project dependency to deps.edn:

```clj
{healthsamurai/matcho {:mvn/version "RELEASE"}}
```

Understand and pick out needed parts:

### Require

```clj
(ns hello-world.core
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [matcho.core :refer :all :as m]))
```

### Spec-like interface

There are three main vars in core ns: `valid?`, `explain-data` and `assert`.
First one is a function, which takes pattern and value returns true if value
conforms the pattern and false in other case. The second one is function, which
returns a vector of errors or nil. The last one is a macro, it works the same
way as `valid?`, but additionally asserts with `is` and provide a
vector of errors using `expalin-data`.

```clj
(m/valid? [int? string?] [1 "test"])
;; => true

(deftest int-str-pair-test
  (m/assert [int? string?] [1 "test"]))

(m/explain-data [int? int? string?] [1 "test"])
;; => [{:expected "#function[clojure.core/int?]", :but "test", :path [1]} {:expected "#function[clojure.core/string?--5132]", :but nil, :path [2]}]

(deftest int-str-pair-fail-test
  (m/assert [int? int? string?] [1 "test"]))

;; [{:expected "#function[clojure.core/int?]", :but "test", :path [1]} {:expected "#function[clojure.core/string?--5132]", :but nil, :path [2]}] [1 "test"] [[#function[clojure.core/int?] #function[clojure.core/int?] #function[clojure.core/string?--5132]]]
```

### How it works?

`Matcho` tooks a pattern and recursevly travers it and related parts of the
provided value. If leaf values looks not so good an error will be added to list
of errors. Value and pattern can have arbitrary nestness.

Pattern can be much smaller (has less keys, elements in vector and so on) than
value, but value will still conform it because `matcho` uses [open-world
assumption](https://en.wikipedia.org/wiki/Open-world_assumption).

```clj
(m/valid? {:status 200} {:status 200 :body "ok"})
;; => true

(m/valid? {:status 200 :body string?} {:status 200})
;; => false
```

### Different leaf values

There are several options for pattern leaf values. It can be:

* Any simple value
* Regular expression
* Clojure spec (keyword or spec directly)
* Predicate (boolean valued function)

```clj
(s/def ::pos-coll (s/coll-of pos?))

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

More advanced examples can be found [here](./test/matcho/core_test.clj).

## Why not just use a clojure.spec?

Because `matcho`, that's why.

```clj
(def response {:status 200
               :body   "ok"})

(deftest with-spec-test
  (s/def ::status #(= 200 %))
  (s/def ::body #(not-empty %))
  (s/def ::response (s/keys :req-un [::status ::body]))
  (is (s/valid? ::response response)))

(deftest without-spec-test
  (m/assert {:status 200 :body not-empty} response))
```

## License

Copyright © 2016 HealthSamurai

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
