var notifyte = angular.module('notifyteApp', ['ngRoute', 'notifyteControllers', 'notifyteServices']);
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
    $routeProvider.otherwise({
      redirectTo: '/'
    });
  }
]);
