(ns bot.telegram
  (:require [morse.handlers :as h]
            [morse.api :as t]
            [morse.polling :as p]
            [clojure.string :as str]))

(def mock-path (System/getenv "TELEGRAM_MOCK_PATH"))

(def token  (or (System/getenv "TELEGRAM_TOKEN")))

(def chatid (or (System/getenv "TELEGRAM_CHATID")))

(defn notify [msg]
  (t/send-text token chatid {:parse_mode "Markdown"} msg))

(defn prepare-text [text]
  (if (or (str/includes? text "_") (str/includes? text "*"))
    (str "```" text "```")
    (str "_" text "_")))

(defn make-error-message [phase text res]
  (let [kind (:kind res)
        res-name (get-in res [:metadata :name])]
    (format "Resource *%s*:*%s* failed phase *%s* with %s"
            kind res-name phase
            (if text (prepare-text text) ""))))

(defn make-success-message [_ text res]
  (let [kind (:kind res)
        res-name (get-in res [:metadata :name])]
    (format "Resource *%s*:*%s* %s"
            kind res-name (prepare-text text))))

(def okEmoji (apply str (Character/toChars 9989)))
(def noEmoji (apply str (Character/toChars 10060)))

(defn notify* [make-message emoji phase text res]
  (let [msg (str emoji " " (make-message phase text res))]
    (if mock-path
      (spit mock-path (str msg "\n") :append true)
      (notify msg))))

(def error (partial notify* make-error-message noEmoji))
(def success (partial notify* make-success-message okEmoji))

; This will define bot-api function, which later could be
; used to start your bot
(h/defhandler bot-api
              ; Each bot has to handle /start and /help commands.
              ; This could be done in form of a function:
              (h/command-fn "start" (fn [{{id :id :as chat} :chat}]
                                      (println "Bot joined new chat: " chat)
                                      (t/send-text token id "Welcome!")))

              ; You can use short syntax for same purposes
              ; Destructuring works same way as in function above
              (h/command "help" {{id :id :as chat} :chat}
                         (println "Help was requested in " chat)
                         (t/send-text token id "Help is on the way"))

              ; Handlers will be applied until there are any of those
              ; returns non-nil result processing update.

              ; Note that sending stuff to the user returns non-nil
              ; response from Telegram API.

              ; So match-all catch-through case would look something like this:
              (h/message message (println "Intercepted message:" message)))


(comment

  ;; start bot api. bot will respond to commands.
  (def channel (p/start token bot-api))

  ;; send message from bot
  (t/send-text token chatid "text message 03")

  (notify "notify test")

  (make-error-message 1 "2" {})

  (success 1 "success message body" {:kind "system" :metadata {:name "name"}}))


