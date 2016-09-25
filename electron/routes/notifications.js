var notification = require('../lib/notification');

var api = module.exports = {};

api.getNotifications = function getNotifications(req, res) {
  res.json(notification.get());
};

api.postNotifications = function postNotifications(req, res) {
  notification.sendNotification(req.body, function(err) {
    if(err) {
      return res.status(400).json({error: err.message});
    }
    res.status(200).json({succes: true});
  });
};

api.delNotifications = function delNotifications(req, res) {
  notification.deleteNotification(req.query, function(err) {
    if(err) {
      return res.status(400).json({error: err.message});
    }
    res.status(200).json({succes: true});
  });
};
