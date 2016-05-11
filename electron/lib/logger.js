var config = require('./config');

var api = module.exports = {};

api.error = function error(msg) {
  if(config.logging.error) {
    console.error(msg);
  }
};

api.info = function info(msg) {
  if(config.logging.info) {
    console.log(msg);
  }
};

api.debug = function debug(msg) {
  if(config.logging.debug) {
    console.log(msg);
  }
};
