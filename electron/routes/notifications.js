var config = require('../lib/config');
var cache = require('memory-cache');
var notification = require('../lib/notification');

var api = module.exports = {};

api.getNotifications = function getNotifications(req, res) {
  res.json(notification.cacheGet());
};

api.postNotifications = function postNotifications(req, res) {
  res.status(200).end();
};
