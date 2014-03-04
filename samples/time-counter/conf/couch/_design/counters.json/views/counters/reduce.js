function(key, values, rereduce) {
  var minutes = 0;
  var name = undefined;
  for(i in values) {
  	var v = values[i];
  	if(v.hasOwnProperty('minutes'))
  	  minutes += v.minutes;
  	if(v.hasOwnProperty('name'))
  	  name = v.name;
  }
  return {"name": name, "minutes": minutes};
}
