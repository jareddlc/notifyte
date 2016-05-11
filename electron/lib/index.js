var async = require('async');
var ble = require('./ble');
var log = require('./logger');
var server = require('./server');

var notifyte = module.exports = {};

notifyte.init = function init(callback) {
  async.waterfall([
    function(callback) {
      ble.init(callback);
    },
    function(callback) {
      server.init(callback);
    }
  ],
  function(err) {
    if(err) {
      log.error('Error: ', err);
    }
    callback(err);
  });
};
