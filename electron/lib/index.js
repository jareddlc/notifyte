var async = require('async');
var ble = require('./ble');
var cache = require('./cache');
var log = require('./logger');
var notification = require('./notification');
var server = require('./server');

var notifyte = module.exports = {};

notifyte.init = function init(callback) {
  var data = {};
  async.waterfall([
    function(callback) {
      cache.init(callback);
    },
    function(callback) {
      data.cache = cache;
      ble.init(data, callback);
    },
    function(callback) {
      data.ble = ble;
      notification.init(data, callback);
    },
    function(callback) {
      server.init(data, callback);
    }
  ],
  function(err) {
    if(err) {
      log.error('Error: ', err);
    }
    callback(err);
  });
};
