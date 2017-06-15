(ns chromex-sample.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [<! chan]]
            [cljs.reader :as reader]
            [cljs.pprint :as pp]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [chromex.ext.tabs :as tabs]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]))

(extend-protocol ISeqable
  js/NodeList
  (-seq [node-list] (array-seq node-list))

  js/HTMLCollection
  (-seq [node-list] (array-seq node-list)))


;; -- right click handler     ------------------------------------------------------------------------------------------------

(defn add-class
  [el class]
  (-> el .-classList (.add class)))

(defn pretty-print-cljs
  [el]
  ;; put text in child code el so that we can get correct clojure highlighting
  (let [text (-> el .-textContent reader/read-string pp/pprint with-out-str)
        code-el (js/document.createElement "code")]
    (add-class code-el "clojure")
    (aset code-el "textContent" text)
    ((aget js/hljs "highlightBlock") code-el)
    (aset el "textContent" "")
    (.appendChild el code-el))
  
  (aset el "style" "white-space" "pre-wrap")
  (add-class el "prettyprint"))

(defn listen-to-page! []
  (let [event-channel (make-chrome-event-channel (chan))]
    (runtime/tap-on-message-events event-channel)
    (go-loop []
      (when-let [event (<! event-channel)]
        (pretty-print-cljs (last (js/document.querySelectorAll ":hover")))))))

;; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (listen-to-page!))
