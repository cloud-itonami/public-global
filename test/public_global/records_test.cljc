(ns public-global.records-test
  "Offline invariants for a repository that is entirely records.

  `tools/verify_citations.cljs` is the LIVE gate: it fetches every citation and
  is therefore worth exactly as much as the network at the moment it runs. It
  cannot answer the questions below, and it stops answering anything at all on
  a plane or behind a proxy — so the two are separate on purpose. This file
  asks only what the bytes in the repository can settle:

  - does every record parse;
  - does the catalogue keep the shape its own gate assumes;
  - is the gate's floor still tied to the catalogue it guards;
  - does the README describe files that are actually here.

  The last one is not hypothetical. When this suite was written the README's
  Project Structure block listed a `wasm/` component with five files under it,
  and `migration.edn` recorded `:go-files-created 0` in the same directory —
  the extraction never carried them. A reader following the README looked for
  a component that had never been in this repository."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            #?(:cljs ["fs" :as fs])))

(defn- slurp* [path]
  #?(:cljs (.readFileSync fs path "utf8")
     :clj (slurp path)))

(defn- exists? [path]
  #?(:cljs (.existsSync fs path)
     :clj (.exists (java.io.File. ^String path))))

(def edn-records ["README.edn" "migration.edn" "deps.edn" "facts/catalog.edn"])
(def json-records ["PROJECT.jsonld" "scheduler.jsonld"])

(defn- catalog [] (edn/read-string (slurp* "facts/catalog.edn")))

(defn- read-or-reason
  "Parse `path` with `f`, or return the reason it could not be parsed.

  The reader is not allowed to throw out of a test. An exception aborts the
  deftest and the report says `1 errors` without naming the file, which is the
  same output a broken harness produces — so a corrupted record and a broken
  suite would look alike from the summary line."
  [path f]
  (try {:ok (f (slurp* path))}
       (catch #?(:clj Exception :cljs :default) e
         {:reason (ex-message e)})))

(deftest every-edn-record-parses
  (doseq [path edn-records]
    (testing path
      (is (exists? path))
      (let [{:keys [ok reason]} (read-or-reason path edn/read-string)]
        (is (map? ok) (str path " must read as a map" (when reason (str " -- " reason))))))))

(deftest every-jsonld-record-parses
  (doseq [path json-records]
    (testing path
      (is (exists? path))
      (let [{:keys [ok reason]}
            (read-or-reason path #?(:cljs #(js/JSON.parse %) :clj identity))]
        (is (some? ok) (str path " must be valid JSON" (when reason (str " -- " reason))))))))

(deftest the-catalogue-keeps-the-shape-its-gate-assumes
  (let [entries (:catalog/entries (catalog))]
    (is (seq entries) "an empty catalogue is not a catalogue")
    (testing "ids are unique -- two rows with one id is one citation wearing two hats"
      (let [ids (map :cite/id entries)]
        (is (= (count ids) (count (distinct ids))))))
    (doseq [{:cite/keys [id url expect-substring]} entries]
      (testing (str id)
        (is (and (string? url) (str/starts-with? url "https://"))
            "a citation must be an https URL")
        (is (and (string? expect-substring) (seq (str/trim expect-substring)))
            "a citation with no expected substring only proves the host answered
             SOMETHING -- a login wall and a data feed both return 200")))))

(deftest the-gates-floor-is-tied-to-the-catalogue-it-guards
  ;; The floor exists so that a catalogue quietly emptied of its rows cannot
  ;; pass as "everything checked". A floor that sits far below the real count
  ;; does not do that: it would have to lose most of the catalogue before it
  ;; noticed. So the floor is declared IN the catalogue and this test is what
  ;; keeps the two from drifting apart.
  (let [{:catalog/keys [entries min-citations]} (catalog)]
    (is (integer? min-citations)
        ":catalog/min-citations must be declared, not left to a flag default")
    (is (>= (count entries) min-citations)
        "the catalogue has fallen below its own floor")
    (is (<= min-citations (count entries))
        "the floor cannot exceed the catalogue -- an unreachable floor fails always")
    (is (>= min-citations (quot (* 3 (count entries)) 4))
        "a floor much lower than the catalogue is a floor that does not notice
         losses -- keep it within a quarter of the real count")))

(defn- structure-block
  "The paths listed in the README's Project Structure fence, with their
  indentation resolved into real relative paths."
  []
  (let [lines (->> (str/split-lines (slurp* "README.md"))
                   (drop-while #(not (str/starts-with? % "## Project Structure")))
                   (drop-while #(not= "```" (str/trim %)))
                   rest
                   (take-while #(not= "```" (str/trim %))))]
    (loop [[line & more] lines stack [] out []]
      (if (nil? line)
        out
        (let [text (str/trim line)]
          (if (str/blank? text)
            (recur more stack out)
            (let [indent (- (count line) (count (str/triml line)))
                  depth (quot indent 2)
                  name* (first (str/split text #"\s+"))
                  stack (conj (vec (take depth stack)) (str/replace name* #"/$" ""))
                  path (str/join "/" stack)]
              (recur more stack (conj out {:path path :entry text})))))))))

(deftest the-readme-lists-only-files-that-are-here
  (let [listed (structure-block)]
    (is (seq listed) "the Project Structure block is empty or was not found --
                      this test cannot pass vacuously")
    (doseq [{:keys [path entry]} listed]
      (is (exists? path)
          (str "README names `" entry "` but " path " is not in this repository")))))
