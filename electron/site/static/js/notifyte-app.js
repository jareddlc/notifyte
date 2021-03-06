var notifyte = angular.module('notifyteApp', ['ngRoute', 'luegg.directives', 'notifyteControllers', 'notifyteServices']);
notifyte.config(function($interpolateProvider) {
  $interpolateProvider.startSymbol('[[').endSymbol(']]');
});

notifyte.config(['$routeProvider', '$locationProvider',
  function($routeProvider, $locationProvider) {
    $locationProvider.html5Mode({
      enabled: false,
      requireBase: false
    });
    var base = '../static/partials';

    $routeProvider.when('/', {
      templateUrl: base + '/settings.html',
    });
    $routeProvider.when('/settings', {
      templateUrl: base + '/settings.html',
    });
    $routeProvider.when('/notification', {
      templateUrl: base + '/notification.html',
    });
    $routeProvider.otherwise({
      redirectTo: '/'
    });
  }
]);
