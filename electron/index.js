var electron = require('electron');
var app = electron.app;
var BrowserWindow = electron.BrowserWindow;
var config = require('./lib/config');
var log = require('./lib/logger');
var mainWindow = null;
var notifyte = require('./lib');

// Quit when all windows are closed.
app.on('window-all-closed', function() {
  app.quit();
});

// Electron is ready. Initialize browser window
app.on('ready', function() {
  // Create the browser window.
  mainWindow = new BrowserWindow(config.window);

  // and load the index.html of the app.
  mainWindow.loadURL('file://' + __dirname + '/site/views/index.html');

  // Open the DevTools.
  // mainWindow.webContents.openDevTools();

  // Emitted when the window is closed.
  mainWindow.on('closed', function() {
    log.info('closed');
    mainWindow = null;
  });
});

// start notifyte
notifyte.init(function() {
  log.info('Notifyte initialized');
});
