var gulp = require('gulp');
var spawn = require('child_process').spawn;
var node;

var ANGULAR = {
  js: {
    files: [
      './bower_components/angular/angular.js',
      './bower_components/angular/angular.min.js',
      './bower_components/angular/angular.min.js.map',
      './bower_components/angular-resource/angular-resource.js',
      './bower_components/angular-resource/angular-resource.min.js',
      './bower_components/angular-resource/angular-resource.min.js.map',
      './bower_components/angular-route/angular-route.js',
      './bower_components/angular-route/angular-route.min.js',
      './bower_components/angular-route/angular-route.min.js.map'
    ],
    dest: './site/static/vendors/js'
  }
};

var BOOTSTRAP = {
  js: {
    files: [
      './bower_components/bootstrap/dist/js/bootstrap.js',
      './bower_components/bootstrap/dist/js/bootstrap.min.js'
    ],
    dest: './site/static/vendors/js'
  },
  css: {
    files: [
      './bower_components/bootstrap/dist/css/bootstrap.css',
      './bower_components/bootstrap/dist/css/bootstrap.min.css',
      './bower_components/bootstrap/dist/css/bootstrap.css.map'
    ],
    dest: './site/static/vendors/css'
  },
  fonts: {
    files: [
      './bower_components/bootstrap/dist/fonts/glyphicons-halflings-regular.eot',
      './bower_components/bootstrap/dist/fonts/glyphicons-halflings-regular.svg',
      './bower_components/bootstrap/dist/fonts/glyphicons-halflings-regular.ttf',
      './bower_components/bootstrap/dist/fonts/glyphicons-halflings-regular.woff'
    ],
    dest: './site/static/vendors/fonts'
  }
};

var FONTAWESOME = {
  css: {
    files: [
      './bower_components/font-awesome/css/font-awesome.css',
      './bower_components/font-awesome/css/font-awesome.min.css',
      './bower_components/font-awesome/css/font-awesome.css.map'
    ],
    dest: './site/static/vendors/css'
  },
  fonts: {
    files: [
      './bower_components/font-awesome/fonts/fontawesome-webfont.eot',
      './bower_components/font-awesome/fonts/fontawesome-webfont.svg',
      './bower_components/font-awesome/fonts/fontawesome-webfont.ttf',
      './bower_components/font-awesome/fonts/fontawesome-webfont.woff',
      './bower_components/font-awesome/fonts/fontawesome-webfont.woff2',
      './bower_components/font-awesome/fonts/FontAwesome.otf'
    ],
    dest: './site/static/vendors/fonts'
  }
};

var JQUERY = {
  js: {
    files: [
      './bower_components/jquery/dist/jquery.js',
      './bower_components/jquery/dist/jquery.min.js',
      './bower_components/jquery/dist/jquery.min.map'
    ],
    dest: './site/static/vendors/js'
  },
};

var WATCH = {
  backend: [
    './lib/**/*',
    './routes/**/*'
  ],
  frontend: [
    './site/**/*'
  ]
};

gulp.task('angular', ['angular-js']);
gulp.task('angular-js', function() {
  return gulp.src(ANGULAR.js.files).pipe(gulp.dest(ANGULAR.js.dest));
});

gulp.task('bootstrap', ['bootstrap-js', 'bootstrap-css', 'bootstrap-fonts']);
gulp.task('bootstrap-js', function() {
  return gulp.src(BOOTSTRAP.js.files).pipe(gulp.dest(BOOTSTRAP.js.dest));
});
gulp.task('bootstrap-css', function() {
  return gulp.src(BOOTSTRAP.css.files).pipe(gulp.dest(BOOTSTRAP.css.dest));
});
gulp.task('bootstrap-fonts', function() {
  return gulp.src(BOOTSTRAP.fonts.files).pipe(gulp.dest(BOOTSTRAP.fonts.dest));
});

gulp.task('fontawesome', ['fontawesome-css', 'fontawesome-fonts']);
gulp.task('fontawesome-css', function() {
  return gulp.src(FONTAWESOME.css.files).pipe(gulp.dest(FONTAWESOME.css.dest));
});
gulp.task('fontawesome-fonts', function() {
  return gulp.src(FONTAWESOME.fonts.files).pipe(gulp.dest(FONTAWESOME.fonts.dest));
});

gulp.task('jquery', ['jquery-js']);
gulp.task('jquery-js', function() {
  return gulp.src(JQUERY.js.files).pipe(gulp.dest(JQUERY.js.dest));
});

gulp.task('bower', ['angular', 'bootstrap', 'fontawesome', 'jquery']);

gulp.task('default', ['watch']);

gulp.task('build');

gulp.task('serve', ['build'], function(cb) {
  if(node) {
    node.kill();
  }
  node = spawn('node', ['index.js'], {stdio: 'inherit'});
  node.on('close', function(code) {
    if(code === 8) {
      console.log('Error detected, waiting for changes...');
    }
  });
  cb(null);
});

gulp.task('watch', ['serve'], function() {
  gulp.watch(WATCH.frontend, ['serve']).on('change', reload);
  gulp.watch(WATCH.backend, ['serve']).on('change', reload);
});

var reload = function reload(event) {
  console.log('File ' + event.path + ' was ' + event.type + ', running tasks...');
};
