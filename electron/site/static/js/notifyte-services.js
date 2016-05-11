var notifyteServices = angular.module('notifyteServices', ['ngResource']);

notifyteServices.factory('notifyteService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

  }
]);

notifyteServices.factory('bluetoothService', ['$rootScope', '$resource', '$timeout',
  function($rootScope, $resource, $timeout) {

    // $resource endpoints
    var bluetoothState = $resource('http://localhost:7777/api/bluetooth/state', {}, {
      get: {method: 'GET', isArray: false}
    });

    var bluetoothAdvertising = $resource('http://localhost:7777/api/bluetooth/advertising', {}, {
      get: {method: 'GET', isArray: false}
    });

    var bluetoothClient = $resource('http://localhost:7777/api/bluetooth/client', {}, {
      get: {method: 'GET', isArray: false}
    });

    // vars
    var REFRESH_RATE = 5000;
    var state = {};
    var advertising = {};
    var client = {};

    // refresh data
    var gBluetoothState = function gBluetoothState() {
      bluetoothState.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(state, json)) {
          angular.copy(json, state);
        }
      });
      $timeout(gBluetoothState, REFRESH_RATE);
    };
    gBluetoothState();

    var gBluetoothAdvertising = function gBluetoothAdvertising() {
      bluetoothAdvertising.get(function(data) {
        var json = data.toJSON();
        if(!angular.equals(advertising, json)) {
          angular.copy(json, advertising);
        }
      });
      $timeout(gBluetoothAdvertising, REFRESH_RATE);
    };
    gBluetoothAdvertising();

    var gBluetoothClient = function gBluetoothClient() {
      bluetoothClient.get(function(data) {
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
