(ns chromex-sample.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.context-menus :as context-menus]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [chromex-sample.background.storage :refer [test-storage!]]))

; -- context menu items

(defn handle-pretty-print-click [menu-info tab-info]
  (let [tab-id (aget tab-info "id")]
    (tabs/execute-script tab-id #js {:file "highlight.js"})
    (tabs/insert-css     tab-id #js {:file "highlight.css"})
    (tabs/send-message   tab-id true)))

(defn create-context-menu-item []
  (context-menus/create #js {:id "pretty-printer"
                             :title "Pretty Print CLJ"
                             :contexts #js ["page" "selection"]}))

; -- main event loop --------------------------------------------------------------------------------------------------------

(defn process-chrome-event [event-num event]
  (let [[event-id event-args] event]
    (if (= event-id ::context-menus/on-clicked)
      (apply handle-pretty-print-click event-args))))

(defn run-chrome-event-loop! [chrome-event-channel]
  (go-loop [event-num 1]
    (when-let [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))))

(defn boot-chrome-event-loop! []
  (create-context-menu-item)
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (context-menus/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (boot-chrome-event-loop!))
