(ns log-scan.core
  (:require [clojure.string :as str])
  (:require [clojure.data.xml :as xml])
  (:import [org.apache.commons.lang3 StringUtils]))

(defn -main
  [& args]
  (def file (slurp "/home/icarus/dev/test.log"))
  (def renders (str/split file #"Executing request startRendering"))
  (def lines (str/split-lines file))
  (def thread-names (distinct (for [line lines] (StringUtils/substringBefore (StringUtils/substringAfter line "[") "]"))))
  (def threads (for [thread thread-names]
                      (filter #(str/includes? % thread) lines)))
  (def xml-report
    (xml/element :report {}
                 (for [render renders]
                   (xml/element :rendering {}
                                (xml/element :document {}(StringUtils/substringBefore (StringUtils/substringAfter render "{arguments=[") ","))
                                (xml/element :page {} (StringUtils/substringBefore (StringUtils/substringAfter render ", ") "]"))
                                (xml/element :uid {} (StringUtils/substringBefore (StringUtils/substringAfter render "Service startRendering returned ") " "))
                                (for [aux renders]
                                  (if (not= aux render)
                                  (xml/element :start {} "2010-10-06 09:03:05,873")))
                                (for [aux renders]
                                  (if (not= aux render)
                                  (xml/element :get {} "2010-10-06 09:03:05,873")))))
                   (xml/element :summary {}
                                (xml/element :count {} (count renders))
                                (xml/element :duplicates {} (count renders))
                                (xml/element :unnecessary {} (count renders)))))
  (println (xml/emit-str xml-report)))
