(ns run
  "nbb entry for the offline record invariants.

  Exits 2 when nothing ran. A harness that finds no tests and reports success
  is indistinguishable from one that found them all and they passed — and this
  repository has exactly one suite, so an empty run is the likeliest way for
  the gate to go quiet without anybody noticing."
  (:require [clojure.test :as t]
            [public-global.records-test]))

(let [{:keys [fail error test]} (t/run-tests 'public-global.records-test)]
  (when (zero? test)
    (println "no tests ran -- refusing to report a pass")
    (js/process.exit 2))
  (js/process.exit (if (pos? (+ fail error)) 1 0)))
