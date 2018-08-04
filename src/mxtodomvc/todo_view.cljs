(ns mxtodomvc.todo-view
  (:require
    [tiltontec.util.core :as util]
    [tiltontec.cell.base :refer [unbound]]
    [tiltontec.cell.observer :refer-macros [fn-obs]]
    [tiltontec.cell.core :refer-macros [cF cF+]]
    [tiltontec.cell.evaluate :refer [not-to-be]]

    [tiltontec.model.core :refer [matrix mx-par <mget ;; mset!> mswap!>
                                  ;;fget mxi-find mxu-find-type
                                  ] :as md]

    [mxxhr.core
     :refer [make-xhr xhr-response]]

    [mxweb.gen
     :refer-macros [label li div input button span]]
    [mxweb.html :as mxweb]

    [mxtodomvc.todo
     :refer [td-title td-created
             td-completed td-delete!
             td-id td-toggle-completed!]]
    [cljs.pprint :as pp]
    [clojure.string :as str]))

;;; -----------------------------------------------------------
;;; --- adverse events ----------------------------------------

(defn de-whitespace [s]
  (str/replace s #"\s" ""))

(def ae-by-brand "https://api.fda.gov/drug/event.json?search=patient.drug.openfda.brand_name:~(~a~)&limit=3")

(defn ae-explorer [todo]
  (button {:class   "li-show"
           :style   (cF (str "display:"
                          (or (when-let [xhr (<mget me :ae)]
                                (let [aes (xhr-response xhr)]
                                  (when (= 200 (:status aes))
                                    "block")))
                            "none")))
           :onclick #(js/alert "Feature not yet implemented.")}

    {:ae (cF+ [:obs (fn-obs
                      ;; we use an observer to GC old XHRs
                      (when-not (or (= old unbound) (nil? old))
                        (not-to-be old)))]
           (when true ;; (<mget (mxweb/mxu-find-class me "ae-autocheck") :on?)
             (make-xhr (pp/cl-format nil ae-by-brand
                         (js/encodeURIComponent (td-title todo)))
               {:name name :send? true})))}

    (span {:style "font-size:0.7em;margin:2px;margin-top:0;vertical-align:top"}
      "View Adverse Events")))


(defn todo-list-item [todo]
  ;; the structure below, and importantly its CSS, was authored
  ;; by the developers of the TodoMVC exercise. Nice!

  ;; Nothing new to note here, except
  (li
    {:class (cF (when (td-completed todo)
                  "completed"))}

    {:todo todo}

    (div {:class "view"}
      (input {:class       "toggle"
              ;; we use namespaced :type to sort out a few things in mxWeb
              ::mxweb/type "checkbox"
              :checked     (cF
                             ;; completed is not a boolean, it is
                             ;; a timestamp that is nil? until the to-do is completed
                             (not (nil? (td-completed todo))))

              ;; td-toggle-completed! expands to an mset!> of the JS epoch or nil
              :onclick     #(td-toggle-completed! todo)})

      (label (td-title todo))

      (ae-explorer todo)

      (button {:class   "destroy"
               ;; we actually have an td-delete! to hide the action, but
               ;; this is a tutorial so let's show the action and use mset!.
               ;; btw, yes, we extend here the spec to support logical deletion
               :onclick #(md/mset!> todo :deleted (util/now))}))))