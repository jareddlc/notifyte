var config = require('./config');

var api = module.exports = {};

api.error = function error(msg, err) {
  if(config.logging.error) {
    if(err) {
      console.error(msg, err);
    }
    else {
      console.error(msg);
    }
  }
};

api.info = function info(msg, data) {
  if(config.logging.info) {
    if(data) {
      console.log(msg, data);
    }
    else {
      console.log(msg);
    }
  }
};

api.debug = function debug(msg, data) {
  if(config.logging.debug) {
    if(data) {
      console.log(msg, data);
    }
    else {
      console.log(msg);
    }
  }
};
