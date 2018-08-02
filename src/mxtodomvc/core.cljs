(ns ^:figwheel-hooks mxtodomvc.core
  (:require
    [goog.dom :as dom]
    [clojure.string :as str]
    [taoensso.tufte :as tufte]
    [tiltontec.cell.core :refer-macros [cFonce]]
    [tiltontec.model.core :refer [<mget] :as md]
    [mxweb.gen :refer-macros [h1 div]]
    [mxweb.html :refer [tag-dom-create]]
    [mxtodomvc.matrix :refer [matrix-build!]]))

(enable-console-print!)
(tufte/add-basic-println-handler! {})

(let [root (dom/getElement "tagroot")
      app-matrix (matrix-build!)]
  (set! (.-innerHTML root) nil)
  (dom/appendChild root
    (tag-dom-create
      (<mget app-matrix :mx-dom)))

  (when-let [route-starter (md/<mget app-matrix :router-starter)]
    (route-starter)))