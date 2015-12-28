(function() {
function BasicParamsViewmodel(target, startseconds, secondsperpoint) {
  var self = this;
  self.target = ko.observable(target);
  self.startseconds = ko.observable(startseconds);
  self.secondsperpoint = ko.observable(secondsperpoint);
  var socket = null;
  self.socket = function(onmessage, onopen, onclose, onerror){
    if (socket == null) {
      socket = new WebSocket("ws://localhost:666/matchmaking/queues/hl/solo?teamsize=5&target=" + self.target() + "&threshold=" + self.startseconds() + "&secondsperpoint=" + self.secondsperpoint());
    }
    socket.onopen = function() {
      var args = arguments;
      onopen.apply(null, args);
      self.running(true);
    }
    socket.onmessage = onmessage;
    socket.onclose = function() {
      var args = arguments;
      onclose.apply(null, args);
      self.running(false);
    }
    socket.onerror = function() {
      var args = arguments;
      onerror.apply(null, args);
      self.running(false);
    }
    return socket;
  }
  self.running = ko.observable(false)
}

function GenerationViewModel(avgmmr, stdmmr, meanstd, seed, speed) {
  var self = this;
  self.avgmmr = ko.observable(avgmmr);
  self.stdmmr = ko.observable(stdmmr);
  self.meanstd = ko.observable(meanstd);
  self.seed = ko.observable(seed);
  self.speed = ko.observable(speed);
  self.generator = new Random(seed);
  self.i = 0;
  var generateOne = function(){
    var mmr = self.generator.normal(parseFloat(self.avgmmr()), parseFloat(self.stdmmr()));
    var sigma = self.generator.gamma(1.3, parseFloat(self.meanstd()));
    var id = self.i;
    self.i++;
    return {
      mmr: mmr,
      sigma: sigma,
      id: id
    };
  }
  self.cancel = false;
  self.intervalid = 0;
  self.generate = function(callback) {
    self.cancel = false;
    window.clearInterval(self.intervalid)
    self.intervalid = window.setInterval(function(){
      if (self.cancel) {
        window.clearInterval(self.intervalid);
      } else {
        var generated = generateOne();
        callback(generated);
      }
    }, 1000 / self.speed());
  }

}

function Player(id, mmr, sigma, secswaited) {
  var self = this;
  self.mmr = Math.floor(mmr);
  self.id = id;
  self.sigma = Math.floor(sigma);
  self.timewaited = secswaited;
  self.minMMR = self.mmr - (2 * self.sigma);
  self.maxMMR = self.mmr + (2 * self.sigma);
}

function Match(team1, team2) {
  var self = this;
  self.team1 = team1;
  self.team2 = team2;
    self.pairs = team1.map(function(e, i, a) { return {
        p1mmr: team1[i].mmr,
        p2mmr: team2[i].mmr,
        p1waited: team1[i].timewaited,
        p2waited: team2[i].timewaited
    };});
}

function Results() {
  var self = this;
  self.waiting = ko.observableArray([]);
  self.found = ko.observableArray([]);
  self.numfound = 0;
}

function ViewModel(basic, generation, results) {
  var self = this;
  self.basic = basic;
  self.generation = generation;
  self.results = results;
  self.socket = null;
  function processgen(gen){
    if(self.socket != null && self.socket.readyState == 1){
      self.results.waiting.push(new Player(gen.id, gen.mmr, gen.sigma, ""));
      self.socket.send(JSON.stringify({
      playerid: gen.id,
      rating: {
        value: Math.floor(gen.mmr),
        uncertainty: Math.floor(gen.sigma)
      }
    }));
    } else {
      console.debug("invalid socket state", self.socket)
    }
  }

  self.runsocket = function(){
    generation.generate(processgen);

    function opening(opening){
      console.debug("socket is open", opening)
      socket = opening.currentTarget;
    }
    
    function receivematch(resp) {
      console.debug("received resp", resp);
      var match = JSON.parse(resp.data);

      var conv = match.map(function(team) {
        return team.map(function(player) {
          return new Player(player.playerid, player.rating.value, player.rating.uncertainty, moment().diff(moment(player.joined), 'seconds'))
        });
      });
      var constructed = new Match(conv[0], conv[1]);
      self.results.found.push(constructed);
      if(self.results.numfound >= 5) {
        self.results.found.shift();
      } else {
        self.results.numfound++;
      }
      match.forEach(function(team){
        team.forEach(function(player) {
          self.results.waiting.remove(function(p) {return p.id == player.playerid});
        });
      });
      console.debug("constructed", constructed);
      //data: "[[{"playerid":28,"rating":{"value":1050.0,"uncertainty":83.0},"joined":"2015-12-28T14:31:11.568Z"},{"playerid":24,"rating":{"value":1195.0,"uncertainty":23.0},"joined":"2015-12-28T14:31:09.568Z"},{"playerid":17,"rating":{"value":1381.0,"uncertainty":52.0},"joined":"2015-12-28T14:31:06.069Z"},{"playerid":25,"rating":{"value":1459.0,"uncertainty":37.0},"joined":"2015-12-28T14:31:10.068Z"},{"playerid":27,"rating":{"value":1531.0,"uncertainty":15.0},"joined":"2015-12-28T14:31:11.068Z"}],[{"playerid":5,"rating":{"value":1086.0,"uncertainty":212.0},"joined":"2015-12-28T14:31:00.068Z"},{"playerid":21,"rating":{"value":1239.0,"uncertainty":39.0},"joined":"2015-12-28T14:31:08.068Z"},{"playerid":7,"rating":{"value":1425.0,"uncertainty":30.0},"joined":"2015-12-28T14:31:01.078Z"},{"playerid":2,"rating":{"value":1493.0,"uncertainty":5.0},"joined":"2015-12-28T14:30:58.569Z"},{"playerid":22,"rating":{"value":1600.0,"uncertainty":75.0},"joined":"2015-12-28T14:31:08.568Z"}]]"

    }
    self.socket = basic.socket(receivematch, opening);
  };

  self.stopgeneration = function(){
    generation.cancel = true;
  }
}

$(function(){
  var myseed = Math.floor(Math.random() * (1000 - 0)) + 0;
  var basic = new BasicParamsViewmodel(40, 400, 3);
  var generation = new GenerationViewModel(2000, 750, 50, myseed, 2);
  var res = new Results();
  var vm = new ViewModel(basic, generation, res);
  ko.applyBindings(vm);
});
}());