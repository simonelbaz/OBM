define(["checkRunner", "pubsub", "checkExternal"], function(checkRunner, pubsub, checkExternal) {
  var startTopic = pubsub.topic("checkScheduler:start");
  var endOfCheckTopic = pubsub.topic("checkScheduler:endOfCheck");
  function checkScheduler ( availableChecks, urlBuilder, checkStartCallback, checkCompleteCallback,  endCallback) {
    this.availableChecks = availableChecks;
    this.urlBuilder = urlBuilder;
    this.flatChecks = this.flattenChecks(availableChecks);
    this.pauseStatusChecks = false;
    this.checkStartCallback = checkStartCallback;
    this.checkCompleteCallback = checkCompleteCallback;
    this.endCallback = endCallback;
  };

  checkScheduler.prototype.flattenChecks = function(availableChecks) {
    var flatChecks = [];
    availableChecks.modules.forEach(function(module) {
      var moduleId = module.id;
      module.checks.forEach(function(check) {
        if ( check.id == "ExternalAccess") {
          flatChecks.push( {moduleId: moduleId, checkId: check.id, moduleUrl: module.url} );
        } else {
          flatChecks.push( {moduleId: moduleId, checkId: check.id} );
        }
      });
    });
    return flatChecks;
  };

  checkScheduler.prototype.runChecks = function() {
    var flatChecks = this.flatChecks;
    var checksCount = this.flatChecks.length;
    var urlBuilder = this.urlBuilder;

    startTopic.publish({checksCount: checksCount});
    this.runOneCheck(flatChecks);
    return checksCount;
  };

  checkScheduler.prototype.pauseChecks = function() {
    this.pauseStatusChecks = true;
  };

  checkScheduler.prototype.resumeChecks = function() {
    this.pauseStatusChecks = false;
    this.runOneCheck(this.flatChecks);
  };

  checkScheduler.prototype.runOneCheck = function(flatChecks) {
    if ( this.flatChecks.length == 0 ) {
      return this.endCallback();
    }
    var nextCheck = this.flatChecks.shift();
    this.checkStartCallback(nextCheck);

    if( nextCheck.moduleUrl ){
      var runner = new checkExternal(nextCheck);
    } else {
      var runner = new checkRunner(nextCheck, this.urlBuilder);
    }

    var self = this;
    runner.run(function(result) {
      self.checkCompleteCallback(nextCheck, result);
      endOfCheckTopic.publish({module: nextCheck.moduleId, check: nextCheck.checkId, result: result});
      if ( !self.pauseStatusChecks ){
        self.runOneCheck(self.flatChecks);
      }
    });
  };

  checkScheduler.prototype.availableChecks = null;
  checkScheduler.prototype.urlBuilder = null;
  checkScheduler.prototype.flatChecks = null;
  
  return checkScheduler;
});