(ns log-scan.core
  (:require [clojure.string :as str])
  (:require [clojure.data.xml :as xml])
  (:import [org.apache.commons.lang3 StringUtils]))

(defn -main
  [& args]
  (def file (slurp "/home/icarus/dev/test.log"))
  (def lines (str/split-lines file))
  (def thread-names (distinct (for [line lines] (StringUtils/substringBefore (StringUtils/substringAfter line "[") "]"))))
  (def threads (for [thread thread-names]
                      (filter #(str/includes? % thread) lines)))
  (def xml-report
    (xml/element :report {}
                 (for [thread threads]
                   (xml/element :rendering {}
                                (for [line thread]
                                  (if (str/includes? line "Executing request startRendering")
                                    (xml/element :document {}(StringUtils/substringBefore (StringUtils/substringAfter line "{arguments=[") ","))))
                                (for [line thread]
                                  (if (str/includes? line "Executing request startRendering")
                                    (xml/element :page {} (StringUtils/substringBefore (StringUtils/substringAfter line ", ") "]"))))
                                (for [line thread]
                                  (if (str/includes? line "Service startRendering returned")
                                    (xml/element :uid {} (StringUtils/substringBefore (StringUtils/substringAfter line "Service startRendering returned ") " "))))
                                (for [aux threads]
                                  (if (not= aux thread) (xml/element :start {} "2010-10-06 09:03:05,873")))
                                (for [aux threads]
                                  (if (not= aux thread) (xml/element :get {} "2010-10-06 09:03:05,873")))))
                   (xml/element :summary {}
                                (xml/element :count {} (count threads))
                                (xml/element :duplicates {} (count threads))
                                (xml/element :unnecessary {} (count threads)))))
  (println (xml/emit-str xml-report)))
