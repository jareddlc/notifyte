var notifyteControllers = angular.module('notifyteControllers', []);

notifyteControllers.controller('notifyteController',  ['$rootScope', '$scope', 'notifyteService',
  function($rootScope, $scope, notifyteService) {

  }
]);

notifyteControllers.controller('bluetoothController',  ['$rootScope', '$scope', 'bluetoothService',
  function($rootScope, $scope, bluetoothService) {

    $scope.$watchCollection(bluetoothService.getBluetooth, function(bluetooth) {
      if(bluetooth) {
        $scope.bluetooth = bluetooth;
      }
    });

  }
]);

notifyteControllers.controller('notificationController',  ['$rootScope', '$scope', '$location', 'notificationService',
  function($rootScope, $scope, $location, notificationService) {
    $scope.currentNotification = [];

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

    $scope.canReply = function canReply() {
      if($scope.currentNotification[0] && $scope.currentNotification[0].reply === true) {
        return true;
      }
      else {
        return false;
      }
    };

    $scope.getPlaceholder = function getPlaceholder() {
      if($scope.currentNotification[0] && $scope.currentNotification[0].reply === false) {
        return 'This application does not support reply';
      }
      else {
        return '';
      }
    };

    $scope.showSettings = function showSettings() {
      $location.path('/settings');
    };

    $scope.showNotification = function showNotification(key) {
      notificationService.setCurrentNotification(key);
      $location.path('/notification');
    };

    $scope.deleteNotification = function deleteNotification($event, key) {
      $event.stopPropagation();
      notificationService.delNotifcation(key);
      $location.path('/');
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
        if(message) {
          notificationService.postNotifcation(message, $scope.currentNotification[0]);
        }
      }
      delete $scope.form;
    };

  }
]);
