var notifyteControllers = angular.module('notifyteControllers', []);

notifyteControllers.controller('notifyteController',  ['$rootScope', '$scope', 'notifyteService',
  function($rootScope, $scope, notifyteService) {

  }
]);

notifyteControllers.controller('bluetoothController',  ['$rootScope', '$scope', 'bluetoothService',
  function($rootScope, $scope, bluetoothService) {

    $scope.$watchCollection(bluetoothService.getBluetoothState, function(state) {
      if(state) {
        $scope.state = state;
      }
    });

    $scope.$watchCollection(bluetoothService.getBluetoothAdvertising, function(advertising) {
      if(advertising) {
        $scope.advertising = advertising;
      }
    });

    $scope.$watchCollection(bluetoothService.getBluetoothClient, function(client) {
      if(client) {
        $scope.client = client;
      }
    });

  }
]);
