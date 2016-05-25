var sockets = require('../lib/socket');

var api = module.exports = {};

api.getSockets = function getSockets(req, res) {
  res.json(sockets.get());
};
