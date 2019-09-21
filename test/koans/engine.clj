(ns koans.engine
  (:require [clojure.test :refer :all])
  )

(def __ :fill-in-the-blank)
(def ___ (fn [& args] __))

(defmacro safe-assert
  "Assertion with support for a message argument in all Clojure
   versions. (Pre-1.3.0, `assert` didn't accept a second argument and
   threw an error.)"
  ([x] `(safe-assert ~x ""))
  ([x msg] `(assert ~x ~msg)))

(defmacro fancy-assert
  "Assertion with fancy error messaging."
  ([x] (fancy-assert x ""))
  ([x message]
   `(try
      (safe-assert (= true ~x) ~message)
      (catch Throwable e#
        (println (str '~message "\n" '~x))))))

(defn ensure-valid-meditation [doc-expression-pairs]
  (doseq [[doc expression] doc-expression-pairs]
    (when-not (string? doc)
      (throw (ex-info (str "Meditations must be alternating doc/expression pairs\n"
                           "Expected " doc " to be a documentation string")
                      {:line (:line (meta doc))}))))
  doc-expression-pairs)


(defmacro meditations [& forms]
  (let [pairs (ensure-valid-meditation (partition 2 forms))
        tests (map (fn [[doc# code#]]
                     `(clojure.test/is ~code# ~doc#))
                   pairs)
        med 'med]
    `(clojure.test/deftest ~med
       ~@tests)))
