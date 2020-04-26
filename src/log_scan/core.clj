(ns log-scan.core
  (:require [clojure.string :as str])
  (:require [clojure.data.xml :as xml]
            [clojure.set :as set])
  (:import [org.apache.commons.lang3 StringUtils]))

(defn -main
  [& args]

  (println "Input file path:")
  (def file-path (read-line))
  (def file (slurp file-path))
  (def lines (str/split-lines file))
  (def uids (distinct (for [line lines]  (StringUtils/substringBefore (StringUtils/substringAfter line "Service startRendering returned ") " "))))
  (def gets (distinct (for [line lines]  (StringUtils/substringBefore (StringUtils/substringAfter line "Executing request getRendering with arguments [") "]"))))
  (def gets-freq (frequencies (set/union uids gets)))
  (def uids-freq (frequencies (for [line lines]  (StringUtils/substringBefore (StringUtils/substringAfter line "Service startRendering returned ") " "))))
  (def thread-names (distinct (for [line lines] (StringUtils/substringBefore (StringUtils/substringAfter line "[") "]"))))
  (def threads (for [thread thread-names] (filter #(str/includes? % (str/join ["[" thread "]"])) lines)))
  (def renders (for [thread threads] (partition-by #(str/includes? % "Service startRendering returned") thread)))


  (def xml-report
    (xml/element :report {}
                 (for [thread renders]
                   (for [render thread]
                     (if (str/includes? (str/join render) "Executing request startRendering")
                       (xml/element :rendering {}
                                    (xml/element :document {}(StringUtils/substringBefore
                                                               (StringUtils/substringAfter
                                                                 (StringUtils/substringAfter
                                                                   (str/join render) "Executing request startRendering") "with arguments [") ","))
                                    (xml/element :page {} (StringUtils/substringBefore
                                                            (StringUtils/substringAfter
                                                              (StringUtils/substringAfter
                                                                (str/join render) "Executing request startRendering")", ") "] "))
                                    (xml/element :uid {} (StringUtils/substringBefore
                                                           (StringUtils/substringAfter
                                                             (StringUtils/substringAfter
                                                               (str/join render) "Executing request startRendering") "{ RenderingCommand - uid: ") " }"))
                                    ;(for [aux threads]
                                    ;  (if (not= aux thread) (xml/element :start {} "2010-10-06 09:03:05,873")))
                                    ;(for [aux threads]
                                    ;  (if (not= aux thread) (xml/element :get {} "2010-10-06 09:03:05,873")))
                                    ) "")))
                 (xml/element :summary {}
                              (xml/element :count {} (reduce +(for [line lines] (if (str/includes? line "Executing request startRendering") 1 0))))
                              (xml/element :duplicates {} (reduce + (for [uid uids-freq] (if (> (get uid 1) 1) 1 0))))
                              (xml/element :unnecessary {} (reduce + (for [gets gets-freq] (if (= (get gets 1) 1) 1 0)))))))
  (println (xml/emit-str xml-report)))

