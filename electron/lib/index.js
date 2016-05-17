var async = require('async');
var ble = require('./ble');
var log = require('./logger');
var notification = require('./notification');
var server = require('./server');

var notifyte = module.exports = {};

notifyte.init = function init(callback) {
  async.waterfall([
    function(callback) {
      ble.init(callback);
    },
    function(callback) {
      server.init(callback);
    },
    function(callback) {
      var data = {ble: ble};
      notification.init(data, callback);
    }
  ],
  function(err) {
    if(err) {
      log.error('Error: ', err);
    }
    callback(err);
  });
};
