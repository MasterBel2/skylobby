(ns spring-lobby.client.handler.tei-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [spring-lobby.client.handler :as handler]
    spring-lobby.client.handler.tei))


(deftest handle-full-queue-list
  (testing "update all matchmaking queues"
    (let [state (atom {:matchmaking-queues {:bleh {:blah :blah}}})]
      (handler/handle nil state "s.matchmaking.full_queue_list 0:1v1\t1:2v2\t2:3v3\t3:ffa")
      (is (= {:matchmaking-queues
              {"0" {:queue-name "1v1"}
               "1" {:queue-name "2v2"}
               "2" {:queue-name "3v3"}
               "3" {:queue-name "ffa"}}}
             @state)))))

(deftest handle-your-queue-list
  (testing "update my matchmaking queues"
    (let [state (atom {:matchmaking-queues
                       {"0" {:queue-name "1v1"
                             :am-in true}
                        "1" {:queue-name "2v2"
                             :am-in true}
                        "2" {:queue-name "3v3"
                             :am-in false}
                        "3" {:queue-name "ffa"}}})]
      (handler/handle nil state "s.matchmaking.your_queue_list 1:2v2\t2:3v3")
      (is (= {:matchmaking-queues
              {"0" {:queue-name "1v1"
                    :am-in false}
               "1" {:queue-name "2v2"
                    :am-in true}
               "2" {:queue-name "3v3"
                    :am-in true}
               "3" {:queue-name "ffa"
                    :am-in false}}}
             @state)))))

(deftest handle-queue-info
  (testing "update matchmaking queue info"
    (let [state (atom {:matchmaking-queues
                       {"0" {:queue-name "1v1"
                             :am-in false}
                        "1" {:queue-name "2v2"
                             :am-in true}
                        "2" {:queue-name "3v3"
                             :am-in true}
                        "3" {:queue-name "ffa"
                             :am-in false}}})]
      (handler/handle nil state "s.matchmaking.queue_info 1\t12345\t987")
      (is (= {:matchmaking-queues
              {"0" {:queue-name "1v1"
                    :am-in false}
               "1" {:queue-name "2v2"
                    :am-in true
                    :current-search-time 12345
                    :current-size 987}
               "2" {:queue-name "3v3"
                    :am-in true}
               "3" {:queue-name "ffa"
                    :am-in false}}}
             @state)))))

(deftest handle-ready-check
  (testing "update matchmaking queue ready check"
    (let [state (atom {:matchmaking-queues
                       {"0" {:queue-name "1v1"
                             :am-in false}
                        "1" {:queue-name "2v2"
                             :am-in true}
                        "2" {:queue-name "3v3"
                             :am-in true}
                        "3" {:queue-name "ffa"
                             :am-in false}}})]
      (handler/handle nil state "s.matchmaking.ready_check 0:1v1")
      (is (= {:matchmaking-queues
              {"0" {:queue-name "1v1"
                    :am-in false
                    :ready-check true}
               "1" {:queue-name "2v2"
                    :am-in true}
               "2" {:queue-name "3v3"
                    :am-in true}
               "3" {:queue-name "ffa"
                    :am-in false}}}
             @state)))))

(deftest handle-cancelled
  (testing "update matchmaking queue match cancelled"
    (let [state (atom {:matchmaking-queues
                       {"0" {:queue-name "1v1"
                             :am-in false
                             :ready-check true}
                        "1" {:queue-name "2v2"
                             :am-in true}
                        "2" {:queue-name "3v3"
                             :am-in true
                             :ready-check true}
                        "3" {:queue-name "ffa"
                             :am-in false}}})]
      (handler/handle nil state "s.matchmaking.match_cancelled 2:3v3")
      (is (= {:matchmaking-queues
              {"0" {:queue-name "1v1"
                    :am-in false
                    :ready-check true}
               "1" {:queue-name "2v2"
                    :am-in true}
               "2" {:queue-name "3v3"
                    :am-in true
                    :ready-check false}
               "3" {:queue-name "ffa"
                    :am-in false}}}
             @state)))))
