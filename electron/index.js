var electron = require('electron');
var app = electron.app;
var BrowserWindow = electron.BrowserWindow;
var config = require('./lib/config');
var log = require('./lib/logger');
var mainWindow = null;
var Menu = electron.Menu;
var MenuItem = electron.MenuItem;
var notifyte = require('./lib');
var remote = electron.remote;

console.dir(electron);

// Quit when all windows are closed.
app.on('window-all-closed', function() {
  app.quit();
});

// Electron is ready. Initialize browser window
app.on('ready', function() {
  // Create the browser window.
  mainWindow = new BrowserWindow(config.window);

  // Load the index.html of the app.
  mainWindow.loadURL('file://' + __dirname + '/site/views/index.html');

  // Open the DevTools.
  // mainWindow.webContents.openDevTools();

  // Menu
  var template = [{
    label: 'Edit',
    submenu: [
      {label: 'Undo', accelerator: 'CmdOrCtrl+Z', selector: 'undo:'},
      {label: 'Redo', accelerator: 'Shift+CmdOrCtrl+Z', selector: 'redo:'},
      {type: 'separator' },
      {label: 'Cut', accelerator: 'CmdOrCtrl+X', selector: 'cut:'},
      {label: 'Copy', accelerator: 'CmdOrCtrl+C', selector: 'copy:'},
      {label: 'Paste', accelerator: 'CmdOrCtrl+V', selector: 'paste:'},
      {label: 'Select All', accelerator: 'CmdOrCtrl+A', selector: 'selectAll:'}
    ]
  }];

  template.unshift({
    label: 'Notifyte',
    submenu: [
      {label: 'About Notifyte', selector: 'orderFrontStandardAboutPanel:' },
      {type: 'separator' },
      {label: 'Quit', accelerator: 'Command+Q', click: function() { app.quit(); }}
    ]
  });

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));

  // Emitted when the window is closed.
  mainWindow.on('closed', function() {
    log.info('closed');
    mainWindow = null;
  });
});

// Start notifyte
notifyte.init(function() {
  log.info('Notifyte initialized');
});
