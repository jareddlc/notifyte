var notifyteServices = angular.module('notifyteServices', ['ngResource']);
var socket = io('http://localhost:7777');
var _ = require('lodash');

notifyteServices.factory('notifyteService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

  }
]);

notifyteServices.factory('bluetoothService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

    // $resource endpoints
    var bluetoothAPI = $resource('http://localhost:7777/api/bluetooth', {}, {
      get: {method: 'GET', isArray: false}
    });

    // socket.io events
    socket.on('onConnect', function(data) {
      $rootScope.$apply(function() {
        socket.emit('/api/bluetooth/', {method: 'GET'});
      });
    });

    socket.on('/api/bluetooth/', function(data) {
      $rootScope.$apply(function() {
        if(!angular.equals(bluetooth, data)) {
          if(data.client !== null && data.client !== bluetooth.client && data.subscribed) {
            var connected = new Notification('Notifyte', {
              body: 'Connected to Phone'
            });
          }

          angular.copy(data, bluetooth);
        }
      });
    });

    // vars
    var REFRESH_RATE = 5000;
    var bluetooth = {};

    // refresh data
    var gBluetooth = function gBluetooth() {
      bluetoothAPI.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(bluetooth, json)) {
          angular.copy(json, bluetooth);
        }
      });
      $timeout(gBluetooth, REFRESH_RATE);
    };
    //gBluetooth();

    // exports
    return {
      getBluetooth: function getBluetooth() {
        return bluetooth;
      }
    };
  }
]);

notifyteServices.factory('notificationService', ['$rootScope', '$resource', '$timeout', '$location',
  function($rootScope, $resource, $timeout, $location) {

    // main window focus
    require('electron').ipcRenderer.on('focus', function(event, message) {
      FOCUS = message;
    });

    // $resource endpoints
    var notificationsAPI = $resource('http://localhost:7777/api/notifications', {}, {
      get: {method: 'GET', isArray: false},
      post: {method: 'POST', isArray: false},
      del: {method: 'DELETE', isArray: false}
    });

    // socket.io events
    socket.on('onConnect', function(data) {
      socket.emit('/api/notifications', {method: 'GET'});
    });

    socket.on('/api/notifications', function(data) {
      $rootScope.$apply(function() {
        if(!angular.equals(notifications, data)) {

          var newNotifcations = angular.copy(data);
          var oldNotifcations = angular.copy(notifications);

          var n = getNotification(newNotifcations, oldNotifcations);
          if(n === null) {
            n = {
              appName: 'Notifyte',
              message: 'New message',
              tag: 'none'
            };
          }
          if(!FOCUS) {
            var connected = new Notification(n.appName, {
              body: n.message,
              tag: n.key
            });
            connected.onclick = function(event) {
              if(event && event.target && event.target.tag) {
                sCurrentNotification(event.target.tag);
                $location.path('/notification');
              }
            };
          }
          angular.copy(data, notifications);
          updateCurrentNotification();
        }
      });
    });

    // vars
    var REFRESH_RATE = 5000;
    var notifications = {};
    var currentNotification = [];
    var FOCUS = false;

    // refresh data
    var gNotifications = function gNotifications() {
      notificationsAPI.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(notifications, json)) {
          angular.copy(json, notifications);
          updateCurrentNotification();
        }
      });
      $timeout(gNotifications, REFRESH_RATE);
    };
    //gNotifications();

    var sCurrentNotification = function sCurrentNotification(key) {
      if(notifications[key]) {
        currentNotification = notifications[key];
      }
    };

    var updateCurrentNotification = function updateCurrentNotification() {
      if(currentNotification && currentNotification.length > 0) {
        angular.forEach(notifications, function(value, key) {
          if(currentNotification[0].key === key) {
            currentNotification = value;
          }
        });
      }
    };

    var pNotifcation = function pNotifcation(message, n) {
      var notification = {
        appName: 'NotifyteDesktop',
        packageName: 'notifyte.desktop',
        key: n.key,
        name: 'Me',
        message: message,
        created: Date.now(),
        replyPackageName: n.packageName,
        replyName: n.name
      };

      notificationsAPI.post(notification, function() {
        console.log('notification sent');
      }, function(err) {
        console.log('err sending', err);
      });
    };

    var dNotification = function dNotification(key) {
      var notification = {
        key: key
      };

      if(notifications[key]) {
        delete notifications[key];
      }
      notificationsAPI.del(notification, function() {
        console.log('notification deleted');
      }, function(err) {
        console.log('err sending', err);
      });
    };

    var getNotification = function getNotification(noti, old) {
      var diff = _.reduce(noti, function(result, value, key) {
        return _.isEqual(value, old[key]) ? result : result.concat(key);
      }, []);

      if(diff && diff.length === 1) {
        return noti[diff][noti[diff].length-1];
      }
      else {
        console.log('receiving bulk notifications');
        return null;
      }
    };

    // exports
    return {
      getNotifications: function getNotifications() {
        return notifications;
      },
      getCurrentNotification: function getCurrentNotification() {
        return currentNotification;
      },
      setCurrentNotification: function setCurrentNotification(key) {
        sCurrentNotification(key);
      },
      postNotifcation: function postNotifcation(message, notification) {
        pNotifcation(message, notification);
      },
      delNotifcation: function delNotifcation(key) {
        dNotification(key);
      }
    };
  }
]);
