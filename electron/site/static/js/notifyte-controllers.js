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

notifyteControllers.controller('notificationController',  ['$rootScope', '$scope', '$location', 'notificationService',
  function($rootScope, $scope, $location, notificationService) {
    $scope.currentNotification = [];
    $scope.hello = 'world';

    $scope.$watchCollection(notificationService.getNotifications, function(notifications) {
      if(notifications) {
        $scope.notifications = notifications;
      }
    });

    $scope.$watchCollection(notificationService.getCurrentNotification, function(notification) {
      if(notification) {
        $scope.currentNotification = notification;
      }
    });

    $scope.showSettings = function showSettings() {
      $location.path('/settings');
    };

    $scope.showNotification = function showNotification(key) {
      notificationService.setCurrentNotification(key);
      $location.path('/notification');
    };

    $scope.isActive = function isActive(key) {
      if($scope.currentNotification.length > 0) {
        if($scope.currentNotification[0].key === key) {
          return true;
        }
      }
      return false;
    };

    $scope.send = function send(message) {
      if($scope.currentNotification.length > 0) {
        notificationService.postNotifcation(message, $scope.currentNotification[0].key);
      }

      delete $scope.form;
    };

  }
]);
