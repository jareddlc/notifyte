var notifyteServices = angular.module('notifyteServices', ['ngResource']);

notifyteServices.factory('notifyteService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

  }
]);

notifyteServices.factory('bluetoothService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

    // $resource endpoints
    var bluetoothStateAPI = $resource('http://localhost:7777/api/bluetooth/state', {}, {
      get: {method: 'GET', isArray: false}
    });

    var bluetoothAdvertisingAPI = $resource('http://localhost:7777/api/bluetooth/advertising', {}, {
      get: {method: 'GET', isArray: false}
    });

    var bluetoothClientAPI = $resource('http://localhost:7777/api/bluetooth/client', {}, {
      get: {method: 'GET', isArray: false}
    });

    // vars
    var REFRESH_RATE = 5000;
    var state = {};
    var advertising = {};
    var client = {};

    // refresh data
    var gBluetoothState = function gBluetoothState() {
      bluetoothStateAPI.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(state, json)) {
          angular.copy(json, state);
        }
      });
      $timeout(gBluetoothState, REFRESH_RATE);
    };
    gBluetoothState();

    var gBluetoothAdvertising = function gBluetoothAdvertising() {
      bluetoothAdvertisingAPI.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(advertising, json)) {
          angular.copy(json, advertising);
        }
      });
      $timeout(gBluetoothAdvertising, REFRESH_RATE);
    };
    gBluetoothAdvertising();

    var gBluetoothClient = function gBluetoothClient() {
      bluetoothClientAPI.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(client, json)) {
          angular.copy(json, client);
        }
      });
      $timeout(gBluetoothClient, REFRESH_RATE);
    };
    gBluetoothClient();

    // exports
    return {
      getBluetoothState: function getBluetoothState() {
        return state;
      },
      getBluetoothAdvertising: function getBluetoothAdvertising() {
        return advertising;
      },
      getBluetoothClient: function getBluetoothClient() {
        return client;
      }
    };
  }
]);

notifyteServices.factory('notificationService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

    // $resource endpoints
    var notificationsAPI = $resource('http://localhost:7777/api/notifications', {}, {
      get: {method: 'GET', isArray: false}
    });

    // vars
    var REFRESH_RATE = 5000;
    var notifications = {};
    var currentNotification = [];

    // refresh data
    var gNotifications = function gNotifications() {
      notificationsAPI.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(notifications, json)) {
          angular.copy(json, notifications);
        }
      });
      $timeout(gNotifications, REFRESH_RATE);
    };
    gNotifications();

    var sCurrentNotification = function sCurrentNotification(key) {
      if(notifications[key]) {
        currentNotification = notifications[key];
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
      }
    };
  }
]);
