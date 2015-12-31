(function() {
  function randomExponential(randomUniform) { return function(rate) {
  var U = randomUniform();
  return -Math.log(U)/rate;
};}

// Geometric random number generator
// Number of failures before the first success, 
// supported on the set {0, 1, 2, 3, ...}
function randomGeometric(randomUniform) {return function(successProbability) {
  var rate = -Math.log(1 - successProbability);
  return Math.floor(randomExponential(randomUniform)(rate));
};}

function BasicParamsViewmodel(target, startseconds, secondsperpoint, adjustMMR) {
  var self = this;
  self.adjustMMR = ko.observable(adjustMMR);
  self.target = ko.observable(target);
  self.startseconds = ko.observable(startseconds);
  self.secondsperpoint = ko.observable(secondsperpoint);
  var socket = null;
  self.socket = function(onmessage, onopen, onclose, onerror){
    var host = function(){
      var loc = window.location, prot;
      if (loc.protocol === "https:") {
        prot = "wss:";
      } else {
        prot = "ws:";
      }
      return prot + "//" + loc.host;
    }();
    if (socket == null) {
      socket = new WebSocket(host + "/matchmaking/queues/hl/solo?teamsize=5&target=" + self.target() + "&threshold=" + self.startseconds() + "&secondsperpoint=" + self.secondsperpoint() + "&adjustmmr=" + self.adjustMMR());
    }
    socket.onopen = function() {
      var args = arguments;
      onopen.apply(null, args);
      self.running(true);
    };
    socket.onmessage = onmessage;
    socket.onclose = function() {
      var args = arguments;
      onclose.apply(null, args);
      self.running(false);
    };
    socket.onerror = function() {
      var args = arguments;
      onerror.apply(null, args);
      self.running(false);
    };
    return socket;
  };
  self.running = ko.observable(false);
}

function GenerationViewModel(avgmmr, stdmmr, avggamesplayed, seed, speed) {
  var self = this;
  self.avgmmr = ko.observable(avgmmr);
  self.stdmmr = ko.observable(stdmmr);
  self.avggamesplayed = ko.observable(avggamesplayed);
  self.seed = ko.observable(seed);
  self.speed = ko.observable(speed);
  self.generator = new Random(seed);
  self.i = 0;
  var generateOne = function(){
    var mmr = self.generator.normal(parseFloat(self.avgmmr()), parseFloat(self.stdmmr()));
    var rate = 1.0 / self.avggamesplayed()
    var totalplayed = randomGeometric(self.generator.random.bind(self.generator))(rate);
    var played = Math.floor(self.generator.random() * totalplayed)
    var sigma = 500 / Math.log( (played * played / 2) + Math.E );
    var id = self.i;
    self.i++;
    return {
      mmr: mmr,
      sigma: sigma,
      id: id
    };
  };
  self.cancel = false;
  self.intervalid = 0;
  self.generate = function(callback) {
    self.cancel = false;
    window.clearInterval(self.intervalid);
    self.intervalid = window.setInterval(function(){
      if (self.cancel) {
        window.clearInterval(self.intervalid);
      } else {
        var generated = generateOne();
        callback(generated);
      }
    }, 1000 / self.speed());
  };

}

function Player(id, mmr, sigma, secswaited) {
  var self = this;
  self.mmr = Math.floor(mmr);
  self.id = id;
  self.sigma = Math.floor(sigma);
  self.queuedAt = moment();
  self.waitingFor = ko.observable("");
  self.timewaited = secswaited;
  self.minMMR = self.mmr - (2 * self.sigma);
  self.maxMMR = self.mmr + (2 * self.sigma);
  self.played = Math.floor(Math.sqrt((2 * Math.exp(500 / sigma)) - (2 * Math.E)));
}

function Match(team1, team2) {
  var self = this;
  self.team1 = team1;
  self.team2 = team2;
    self.pairs = team1.map(function(e, i, a) { return {
        p1mmr: team1[i].mmr,
        p2mmr: team2[i].mmr,
        p1played: team1[i].played,
        p2played: team2[i].played,
        p1minmmr: team1[i].minMMR,
        p1maxmmr: team1[i].maxMMR,
        p2minmmr: team2[i].minMMR,
        p2maxmmr: team2[i].maxMMR,
        p1waited: team1[i].timewaited,
        p2waited: team2[i].timewaited
    };});
}

function Results() {
  var self = this;
  self.waiting = ko.observableArray([]);
  self.found = ko.observableArray([]);
  self.numfound = 0;
  self.numwaiting = ko.computed(function(){
    return self.waiting().length;
  }, self).extend({rateLimit: 1000});
  window.setInterval(function(){
    var now = moment();
    self.waiting().forEach(function(e, i, a){
      e.waitingFor(e.queuedAt.from(now));
    });
  }, 1000);
}

function ViewModel(basic, generation, results) {
  var self = this;
  self.basic = basic;
  self.generation = generation;
  self.results = results;
  self.socket = null;
  self.running = ko.observable(false);
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
      console.debug("invalid socket state", self.socket);
    }
  }

  self.runsocket = function(){
    generation.generate(processgen);

    function opening(open){
      self.running(true);
      console.debug("socket is open", open);
      socket = open.currentTarget;
    }

    function socketclosed() {
      alert("connection to server closed. Refresh the page to restart the fun");
    }
    
    function receivematch(resp) {
      var match = JSON.parse(resp.data);

      var conv = match.map(function(team) {
        return team.map(function(player) {
          return new Player(player.playerid, player.rating.value, player.rating.uncertainty, moment().diff(moment(player.joined), 'seconds'));
        });
      });
      var constructed = new Match(conv[0], conv[1]);
      self.results.found.unshift(constructed);
      if(self.results.numfound >= 5) {
        self.results.found.pop();
      } else {
        self.results.numfound++;
      }
      match.forEach(function(team){
        team.forEach(function(player) {
          self.results.waiting.remove(function(p) {return p.id == player.playerid;});
        });
      });
    }
    self.socket = basic.socket(receivematch, opening, socketclosed, socketclosed);
  };

  self.stopgeneration = function(){
    self.running(false);
    generation.cancel = true;
  };
}

$(function(){
  var myseed = Math.floor(Math.random() * (1000 - 0)) + 0;
  var basic = new BasicParamsViewmodel(40, 400, 3, 2);
  var generation = new GenerationViewModel(2000, 750, 2000, myseed, 2);
  var res = new Results();
  var vm = new ViewModel(basic, generation, res);
  ko.applyBindings(vm);
});
}());