(ns mxtodomvc.build
  (:require
    [tiltontec.model.core :refer [<mget] :as md]
    [mxweb.gen
     :refer-macros [section header h1 footer p]]))

;; Below we demonstrate the HTML work-alike quality of mxWeb, and that
;; CLJS can be coded naturally as needed.
;;
;; A nice win of everything just being CLJS is that we get "Web Components"
;; for free. This first "component" function is rather simple, but of course
;; could take as many parameters as needed to be as reusable as needed.

(defn mxtodo-credits []
  (footer {:class "info"}
    ;;
    ;; yes, we can mix CLJS with the pseudo-HTML functions,
    ;; simply because in mxWeb everythig is CLJS;
    ;;
    ;; here "children" will be a single vector. We could also
    ;; toss in another child and/or a nil and/or a nested vector.
    ;; It all gets flattened with nils pruned.
    ;;
    (for [credit ["Double-click a to-do list item to edit it."
                  "Created by <a href=\"https://github.com/kennytilton\">Kenneth Tilton</a>."
                  "Inspired by <a href=\"https://github.com/tastejs/todomvc/blob/master/app-spec.md\">TodoMVC</a>."]]
      (p credit))))

(defn matrix-build []
  (md/make
    ;;
    ;; HTML tag syntax is (<tag> [dom-attribute-map [custom-property map] children*]
    ;;
    :mx-dom (section {:class "todoapp"}
              (header {:class "header"}
                (h1 "todos")
                (mxtodo-credits)))))