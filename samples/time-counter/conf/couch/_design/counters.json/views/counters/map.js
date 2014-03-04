function(doc){
  if(doc.type === "counter")
  	emit(doc._id, {name: doc.name}) 
  else if(doc.type === "increment")
    emit(doc.counterId, {minutes: doc.minutes})
}
