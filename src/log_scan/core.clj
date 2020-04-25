(ns log-scan.core
  (:require [clojure.string :as str])
  (:require [clojure.data.xml :as xml])
  (:import [org.apache.commons.lang3 StringUtils]))

(defn -main
  [& args]

  (println "Input file path:")
  ;(def file-path (read-line))
  ;(def file (slurp file-path))
  (def file (slurp "/home/icarus/dev/test.log"))
  (def lines (str/split-lines file))
  (def uids (frequencies (for [line lines]  (StringUtils/substringBefore (StringUtils/substringAfter line "Service startRendering returned ") " "))))
  (def thread-names (distinct (for [line lines] (StringUtils/substringBefore (StringUtils/substringAfter line "[") "]"))))
  (def threads (for [thread thread-names] (filter #(str/includes? % (str/join ["[" thread "]"])) lines)))
  (def renders (for [thread threads] (partition-by #(str/includes? % "Executing request startRendering") thread)))


  (def xml-report
    (xml/element :report {}
                 (for [thread renders]
                   (for [render thread]
                     (xml/element :rendering {}
                                  (for [line render]
                                    (if (str/includes? line "Executing request startRendering")
                                      (xml/element :document {}(StringUtils/substringBefore (StringUtils/substringAfter line "with arguments [") ","))))
                                  (for [line render]
                                    (if (str/includes? line "Executing request startRendering")
                                      (xml/element :page {} (StringUtils/substringBefore (StringUtils/substringAfter line ", ") "]"))))
                                  (for [line render]
                                    (if (str/includes? line "Service startRendering returned")
                                      (xml/element :uid {} (StringUtils/substringBefore (StringUtils/substringAfter line "Service startRendering returned ") " "))))
                                  ;(for [aux threads]
                                  ;  (if (not= aux thread) (xml/element :start {} "2010-10-06 09:03:05,873")))
                                  ;(for [aux threads]
                                  ;  (if (not= aux thread) (xml/element :get {} "2010-10-06 09:03:05,873")))
                                  ) ))
                 (xml/element :summary {}
                              (xml/element :count {} (reduce +(for [line lines] (if (str/includes? line "Executing request startRendering") 1 0))))
                              (xml/element :duplicates {} (reduce + (for [uid uids] (if (> (get uid 1) 1) 1 0))))
                              (xml/element :unnecessary {} (count threads)))))
  (println (xml/emit-str xml-report)))

