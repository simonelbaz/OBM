/******************************************************************************
 * Calendar event object, can be dragged but have no graphical representation
 * of it's duration.
 ******************************************************************************/
Obm.CalendarDayEvent = new Class({
  
  setOptions: function(options){
    this.options = Object.extend({
      draggable: false,
      type: 'day',
      yUnit: 0,
      xUnit: 24*3600,
      unit : 24*3600,
      context: obm.calendarManager.headContext
    }, options || {});
  },

  
  initialize: function(eventData,options) {;
    this.setOptions(options);
    this.event = eventData;
    this.buildEvent();
    this.size = 1;
    this.hidden = 0;
    if(this.options.draggable)
      this.makeDraggable();    
    this.setTime(this.event.time);
    this.setDuration(this.event.duration);
  },

  makeDraggable: function() {
    var dragOptions = {
      handle: this.dragHandler,
      xMin: this.options.context.left,
      xMax: this.options.context.right - obm.calendarManager.defaultWidth,
      yMin: this.options.context.top,
      yMax: this.options.context.bottom - this.element.offsetHeight,

      onStart:function() {
        obm.calendarManager.lock();
        this.element.setStyle('width', obm.calendarManager.defaultWidth + 'px');
        this.element.setStyle('margin-left', '0');
        this.element.setOpacity(.7);
      },

      onComplete:function() {
        this.element.setOpacity(1);
        obm.calendarManager.moveEventTo(this.element.id,this.element.getLeft(),this.element.getTop());
        obm.calendarManager.unlock()
      }     
    };   

    this.drag = this.element.makeDraggable(dragOptions);
  },

  buildEvent: function() {
    id = this.event.id+'-'+this.event.entity+'-'+this.event.entity_id+'-'+this.event.time;
    this.element = new Element('div').addClassName('event')
                                     .addClassName(this.event.class)
                                     .setProperty('id','event-'+id)
                                     .injectInside(document.body);
    this.dragHandler = new Element('h1')
                                 .injectInside(this.element);     
    if(this.event.meeting) {                                 
      new Element('img').setProperty('src',obm.vars.images.meeting)
                        .injectInside(this.dragHandler);
    }
    if(this.event.periodic) {                                 
      new Element('img').setProperty('src',obm.vars.images.periodic)
                        .injectInside(this.dragHandler);
    }   
    this.titleContainer = new Element('span').injectInside(this.dragHandler);
    this.resetTitle();

  },
  
  resetTitle: function() {
    this.setTitle(this.event.title);
  },

  setTitle: function(title) {
    this.event.title = title;
    this.titleContainer.setHTML(this.event.title);
  },

  setTime: function(time) {
    origin = (time - obm.calendarManager.startTime) - (time - obm.calendarManager.startTime)%(this.options.xUnit||1);
    if(this.setOrigin(origin)) {
      if(this.event.time) {
        time = new Date(time * 1000);
        d = new Date(this.event.time * 1000);
        time.setHours(d.format('H'));
        time.setMinutes(d.format('i'));
        time = time.getTime()/1000;
      };
      this.event.time = time;
    } else {
      this.redraw();
    }

  },

  setOrigin: function(origin) {
    if(origin < 0) {   
      this.hidden = origin / this.options.xUnit;
      origin = 0;
    } else if(this.hidden != 0) {
        this.hidden = 0;
        this.setSize(this.size);
    }
    head = $('calendarHead');
    hr = $(this.options.type+'-'+origin);
    if(head && typeof(this.origin) == 'undefined') {
       hr.parentNode.setStyle('height', (hr.parentNode.offsetHeight + this.element.offsetHeight) + 'px');
    }
    this.origin = origin;
    this.redraw();
    if(obm.calendarManager.lock()) {
      obm.calendarManager.resizeWindow();
      obm.calendarManager.unlock()
    }    
    return true;
  },

  setDuration: function(duration) {
    this.event.duration = duration;
    date_begin = new Date(this.event.time * 1000);
    date_end = new Date((this.event.time + this.event.duration) * 1000);
    this.setSize(date_end.format('d') - date_begin.format('d') + 1);
  },

  setSize: function(size) {
    this.size = size + this.hidden;
    this.setWidth(this.size * obm.calendarManager.defaultWidth);
    if(this.drag) {
      this.drag.options.xMax = this.options.context.right - obm.calendarManager.defaultWidth;
    }
  },

  redraw: function() {
    hr = $(this.options.type+'-'+this.origin);
    this.element.setStyles({
      'top':  hr.getTop() + hr.getStyle('padding-top').toInt() + 'px',
      'left': hr.getLeft() + 'px'
    });
    this.setWidth(this.size * obm.calendarManager.defaultWidth);
    if(this.options.draggable) {
      this.drag.options.xMin = this.options.context.left;
      this.drag.options.xMax = this.options.context.right - obm.calendarManager.defaultWidth;
      this.drag.options.yMin = this.options.context.top;
      this.drag.options.yMax = this.options.context.bottom - this.element.offsetHeight;
    }
  },

  setWidth: function(width) {
    if( (this.element.offsetLeft + width) > this.options.context.right ) {
      width = this.options.context.right - this.element.offsetLeft;
    }
    this.element.setStyle('width',width + 'px');
  },

  conflict: function(size, position) {
    this.element.setStyle('margin-top',position * this.element.offsetHeight + 'px');
  },

  toQueryString: function() {
    date_begin = new Date(this.event.time * 1000);
    date_end = new Date(this.event.time * 1000 + this.event.duration * 1000);    
    query = 'calendar_id=' + this.event.id;
    query += '&date_begin=' + date_begin.format('Y-m-d H:i:s');
    query += '&duration=' + this.event.duration;
    query += '&title=' + this.event.title;
    return query;
  }

})

/******************************************************************************
 * Extended event with graphical representation of it's duration and which
 * could be resize
 *****************************************************************************/

Obm.CalendarEvent = Obm.CalendarDayEvent.extend({

  setOptions: function(options){
    this.options = Object.extend({
      resizable: false,
      type: 'time',
      yUnit: obm.vars.consts.timeUnit,
      xUnit: 3600*24,
      unit : obm.vars.consts.timeUnit,
      context: obm.calendarManager.bodyContext
    }, options || {});
  },

  initialize: function(eventData,options) {
    this.setOptions(options);
    this.event = eventData;
    this.buildEvent();
    //this.element = $('event-'+this.event.id+'-'+this.event.entity+'-'+this.event.entity_id+'-'+this.event.time);
    if(this.options.resizable) 
      this.makeResizable(options);
    if(this.options.draggable)
      this.makeDraggable();      
    this.setTime(this.event.time);
    this.setDuration(this.event.duration);          
  },
  
  buildEvent: function() {
    id = this.event.id+'-'+this.event.entity+'-'+this.event.entity_id+'-'+this.event.time;
    this.element = new Element('div').addClassName('event')
                                     .addClassName(this.event.class)
                                     .setProperty('id','event-'+id)
                                     .injectInside(document.body);
    this.dragHandler = new Element('h1')
                                 .injectInside(this.element);     
    if(this.event.meeting) {                                 
      new Element('img').setProperty('src',obm.vars.images.meeting)
                        .injectInside(this.dragHandler);
    }
    if(this.event.periodic) {                                 
      new Element('img').setProperty('src',obm.vars.images.periodic)
                        .injectInside(this.dragHandler);
    }   
    if(this.options.resizable) {
      this.resizeHandler = new Element('img').setProperty('src',obm.vars.images.resize)
                                             .addClassName('handle')
                                             .injectInside(this.element);
    }
    this.titleContainer = new Element('span').injectInside(this.element);
    this.timeContainer = new Element('span').injectInside(this.dragHandler);
    this.resetTitle();

  },

  setDuration: function(duration) {
    this.event.duration = duration;
    size = Math.ceil(duration/this.options.yUnit);
    this.setSize(size);
  },

  setSize: function(size) {
    this.size = size;
    height = size * obm.calendarManager.defaultHeight;
    this.setHeight(height);
    if(this.resize) {
      this.resize.options.yMax = this.options.context.bottom - this.element.getTop();
    }
    if(this.drag) {
      this.drag.options.yMax = this.options.context.bottom - this.element.offsetHeight;
    }
  },

  setHeight: function(height) {
    if((this.element.offsetTop + height) > this.options.context.bottom) {
      height = this.options.context.bottom - this.element.offsetTop;
    }
    this.element.setStyle('height',height + 'px');
  },

  makeResizable: function() {
    var resizeOptions = {
      handle: this.resizeHandler,
      xMin: obm.calendarManager.defaultWidth,
      xMax: obm.calendarManager.defaultWidth,
      yMin: obm.calendarManager.defaultHeight,
      yMax: this.options.context.bottom - this.element.getTop(),

      onStart:function() {
        //this.element.setStyle('width', obm.calendarManager.defaultWidth + 'px');
        this.element.setStyle('margin-left', '0');
        this.element.setOpacity(.7);
      },
      
      onComplete:function() {
        this.element.setOpacity(1);
        obm.calendarManager.resizeEventTo(this.element.id,this.element.offsetHeight);
      }     
    };       

    this.resize = this.element.makeResizable(resizeOptions);
  },

  setMargin: function(margin) {
    this.element.setStyle('margin-left',margin + 'px');
  },
 

  setTime: function(time) {
    origin = time - (time - obm.calendarManager.startTime)%this.options.yUnit;
    if(this.setOrigin(origin)) {
      this.event.time = time;
      this.timeContainer.setHTML(new Date(this.event.time * 1000).format("H:i"));
    } else {
      this.redraw();
    }
  },

  setOrigin: function(origin) {
    hr = $(this.options.type+'-'+origin);
    if(hr) {
      this.origin = origin;
      this.element.remove();
      hr.adopt(this.element);
      this.redraw();
      return true;
    } else {
      return false;
    }
  },

  redraw: function() {
    hr = $(this.options.type+'-'+this.origin);
    this.element.setStyles({
      'top':  hr.getTop() + 'px',
      'left': hr.getLeft()  + 'px'      
    });
    this.setHeight(this.size * obm.calendarManager.defaultHeight);
    if(this.options.draggable) {
      this.drag.options.xMin = this.options.context.left;
      this.drag.options.xMax = this.options.context.right - obm.calendarManager.defaultWidth;
      this.drag.options.yMin = this.options.context.top;
      this.drag.options.yMax = this.options.context.bottom - this.element.offsetHeight;
    }    
    if(this.options.resizable) {
      this.resize.options.xMin = obm.calendarManager.defaultWidth;
      this.resize.options.xMax = obm.calendarManager.defaultWidth;
      this.resize.options.yMin = obm.calendarManager.defaultHeight;
      this.resize.options.yMax = this.options.context.bottom - this.element.getTop();
    }
  },

  conflict: function(size, position) {
    if(size > 1)
      this.setWidth(obm.calendarManager.defaultWidth/size);
    else
      this.setWidth(obm.calendarManager.defaultWidth);
    this.setMargin((obm.calendarManager.defaultWidth/size)*position);
  }

});

/******************************************************************************
 * Calendar Manager which redraw all events, and is a kind of home for the
 * events objects.
 ******************************************************************************/

Obm.CalendarManager = new Class({

  initialize: function(startTime) {
    this.startTime = startTime;
    this.events = new Hash();
    this.times = new Hash();
    this.redrawLock = false;

    ctx = $('calendarEventContext');
    head = $('calendarHead');
    body = $('calendarBody');

    this.headContext = new Object();
    if(head) {
      this.headContext.top = head.getTop();
      this.headContext.right = ctx.getLeft() + ctx.offsetWidth;
      this.headContext.left = ctx.getLeft();
      this.headContext.bottom = head.getTop() + head.offsetHeight;    
    }

    this.bodyContext = new Object();
    this.bodyContext.top = body.getTop();
    this.bodyContext.right = ctx.getLeft() + ctx.offsetWidth;
    this.bodyContext.left = ctx.getLeft();
    this.bodyContext.bottom = body.getTop() + body.offsetHeight;
    
    this.evidence = $E('td',body);
  
    this.evidence.observe({onStop:this.resizeWindow.bind(this), property:'offsetWidth'});
    this.evidence.observe({onStop:this.resizeWindow.bind(this), property:'offsetTop'});

    this.defaultWidth = this.evidence.clientWidth;
    this.defaultHeight = this.evidence.offsetHeight; 
  },

  lock: function() {
    if(!this.redrawLock)
      return (this.redrawLock = true);
    else 
      return false;
  },

  unlock: function() {
    this.redrawLock = false;
  },

  newDayEvent: function(eventData,options) {
    obmEvent = new Obm.CalendarDayEvent(eventData,options);
    this.events.put(obmEvent.element.id,obmEvent);
    this.register(obmEvent.element.id);
  },

  newEvent: function(eventData,options) {
    obmEvent = new Obm.CalendarEvent(eventData,options);
    this.events.put(obmEvent.element.id,obmEvent);
    this.register(obmEvent.element.id);
  },

  resizeWindow: function() {
    if(this.lock()) {

      ctx = $('calendarEventContext');
      head = $('calendarHead');
      body = $('calendarBody');

      if(head) {
        this.headContext.top = head.getTop();
        this.headContext.right = ctx.getLeft() + ctx.offsetWidth;
        this.headContext.left = ctx.getLeft();
        this.headContext.bottom = head.getTop() + head.offsetHeight;    
      }

      this.bodyContext.top = body.getTop();
      this.bodyContext.right = ctx.getLeft() + ctx.offsetWidth;
      this.bodyContext.left = ctx.getLeft();
      this.bodyContext.bottom = body.getTop() + body.offsetHeight;      
  
      
      this.defaultWidth = this.evidence.clientWidth;
      this.defaultHeight = this.evidence.offsetHeight;    

      this.events.each(function(key,evt) {
        evt.redraw(); 
      });
      
      this.redrawAllEvents();
      this.unlock();
   }
  },


  compareEvent: function(event1, event2) {
    diff = event1.event.time - event2.event.time;
    if(diff != 0)
      return diff;
    diff = event1.event.id - event2.event.id;
    if(diff != 0)
      return diff;
    return event1.event.entity_id - event2.event.entity_id;
  },

  compareTime: function(time1, time2) {
    return time1.toInt() - time2.toInt();
  },

  moveEventTo: function(id,left,top) {
    evt = this.events.get(id);
    xDelta = Math.round((left-evt.options.context.left)/this.defaultWidth);
    yDelta = Math.floor((top-evt.options.context.top)/this.defaultHeight);
    time = this.startTime + xDelta*evt.options.xUnit + yDelta*evt.options.yUnit;
    if(evt.event.time != time) {
//      this.unregister(id);
//      evt.setTime(time);
//      this.register(id);   
      eventData = new Object();
      eventData.calendar_id = evt.event.id;
      eventData.element_id = id;
      eventData.date_begin = new Date(time * 1000).format('Y-m-d H:i:s');
      eventData.duration = evt.event.duration;
      eventData.title = evt.event.title;
      this.sendUpdateEvent(eventData);
//      this.redrawAllEvents(evt.event.time);
    } else {
      evt.redraw();
      this.redrawAllEvents(evt.event.time);
    }
  },

  resizeEventTo: function(id,size) {
    size = Math.round(size/this.defaultHeight);
    evt = this.events.get(id);
    if(size != evt.size) {
//      this.unregister(id);
//      evt.setDuration(size*evt.options.yUnit);
//      this.register(id);   
      eventData = new Object();
      eventData.calendar_id = evt.event.id;
      eventData.element_id = id;
      eventData.date_begin = new Date(evt.event.time * 1000).format('Y-m-d H:i:s');
      eventData.duration = size*evt.options.yUnit;
      eventData.title = evt.event.title;
      this.sendUpdateEvent(eventData);
//      this.redrawAllEvents(evt.event.time);
    } else {
      evt.redraw();
      this.redrawAllEvents(evt.event.time);
    }
  },

  register: function(id) {
    evt = this.events.get(id);
    size = evt.size;
    for(var i=0;i < size;i++) {
      t = evt.origin + i*evt.options.unit;
      if(!this.times.get(t)) {
        this.times.put(t.toInt(),new Array());
      }
      this.times.get(t).push(evt);
    }   
  },

  unregister: function(id) {
    size = evt.size;
    for(var i=0;i < size;i++) {
      t = evt.origin + i*evt.options.unit;
      this.times.get(t).remove(evt);
    }
  },

  redrawAllEvents: function() {
    keys = this.times.keys().sort(this.compareTime);
    this.redrawEvents(keys);
  }, 

  redrawEvents: function(keys) {
    resize = new Object();
    end = 0;
    for(var k=0;k < keys.length; k++) {
      time = this.times.get(keys[k]);
      if(time.length == 0)
        continue;
      if(end == 0) {
        unit = new Object()
        unit.size = 1;
      }
      if(unit.size < time.length) {
        unit.size = time.length;
      }
      time.sort(this.compareEvent);
      //TODO Revoir la gestion de la premiere 
      //place disponible
      free = new Array;
      for(var i=0;i<time.length;i++) {
        free.push(i);
      }
      // PERFORMING Event position
      for(var i=0;i<time.length;i++) {
        evt = time[i];
        id = evt.element.id;        
        if(keys[k] == evt.origin) {
          size = evt.size;
          resize[id] = new Object;
          resize[id].position = free.splice(0, 1)[0];
          resize[id].unit = unit;      
          if(size > end) {
            end = size;
          }
        } else  {
          free.remove(resize[id].position);
        }
      }
      end --;
    }
    // REDRAWING EVENTS
    for(var i in resize) {
      evt = this.events.get(i);
      evt.conflict(resize[i].unit.size,resize[i].position);
    }
  },
  
  sendUpdateEvent: function(eventData) {
    evt = this.events.get(id);
    query = evt.toQueryString();
    ajax = new Ajax('calendar_index.php',
    {postBody:'ajax=1&action=quick_update&' + Object.toQueryString(eventData), onComplete: this.receiveUpdateEvent, method: 'post'});
    ajax.request();
  },
  
  receiveUpdateEvent: function(request) {
    try {
      resp = eval(request);
    } catch (e) {
      resp = new Object();
      resp.error = 1;
      resp.message = 'Fatal server error, please reload';
    }
    events = resp.eventsData;
    if(resp.error == 0) {
      showOkMessage(resp.message);
      str = resp.elementId.split('-');
      for(var i=0;i< events.length;i++) {
        event = events[i].event;
        id = str[0]+'-'+str[1] +'-'+event.entity+'-'+event.entity_id+'-'+str[4];
        evt = obm.calendarManager.events.get(id);
        if(event.state == 'A') {
          obm.calendarManager.unregister(id);
          evt.setTime(event.time);
          evt.setDuration(event.duration);
          obm.calendarManager.register(id);           
          evt.setTitle(event.title);
        } else if (evt) {
          obm.calendarManager.unregister(id);
          obm.calendarManager.events.remove(id);
          ev.destroy();
        }
      }
      obm.calendarManager.redrawAllEvents();      
    } else {
      showErrorMessage(resp.message);
      obm.calendarManager.events.each(function(key,evt) {
        evt.redraw(); 
      });      
    }
    
  },

  sendCreateEvent: function(eventData) {
    ajax = new Ajax('calendar_index.php',
    {postBody:Object.toQueryString(eventData), onComplete: this.receiveCreateEvent, method: 'post'});
    ajax.request();
  },

  receiveCreateEvent: function(request) {
    resp = eval(request);
    events = resp.eventsData;
    if(resp.error == 0) {
      showOkMessage(resp.message);
      if(resp.day == 'true') {
        obm.calendarManager.newDayEvent(events[0].event,events[0].options);
      } else {
        obm.calendarManager.newEvent(events[0].event,events[0].options);
      }
      obm.calendarManager.redrawAllEvents();      
    } else {
      showErrorMessage(resp.message);
    }
  }
});


Obm.CalendarQuickForm = new Class({
  initialize: function() {
    this.popup = $('calendarQuickForm');
    this.date = $('calendarQuickFormDate');
    this.form = $('calendarQuickFormStore');
    this.popup.setStyle('position','absolute');
  },
  
  compute: function(evt, context) {
    target = evt.target || evt.srcElement
    if(obm.calendarManager.redrawLock || target.id == '') {
      return false;
    }
    str = target.id.split('-');
    type = str[0];
    if(type == 'time' || type == 'hour') {
      this.setDefaultFormValues(str[1].toInt(),0, context);
    } else {
      elId = 'event-' + str[1] + '-' + str[2] + '-' + str[3] + '-' + str[4];
      evt = obm.calendarManager.events.get(elId);
      target = evt.element;
      this.setFormValues(evt,context);
    }
    this.show();    
    left = target.getLeft() - Math.round((this.popup.offsetWidth - target.offsetWidth)/2);
    top = target.getTop() - this.popup.offsetHeight + Math.round(target.offsetHeight/2);;
    this.popup.setStyles({
      'top':  top + 'px',
      'left': left  + 'px'
    });    
  }, 
  
  setFormValues: function(evt, context) {
    date_begin = new Date(evt.event.time * 1000);
    date_end = new Date((evt.event.time + evt.event.duration) * 1000);    
    this.form.tf_title.value = evt.event.title;
    this.form.event_id.value = evt.event.id;
    this.form.entity.value = evt.event.entity;
    this.form.entity_id.value = evt.event.entity_id;
    this.form.all_day.value = evt.event.all_day;
    this.form.action.value = 'quick_update';
    this.form.date_begin.value = date_begin.format('Y-m-d H:i:s');
    this.form.duration.value = evt.event.duration;
    this.form.context.value = context;
    this.form.element_id.value = evt.element.id;
    this.date.innerHTML = date_begin.format('Y/m/d H:i') + '-' + date_end.format('Y/m/d H:i');
  },

  setDefaultFormValues: function(time, allDay,context) {
    date_begin = new Date(time * 1000);
    date_end = new Date((time + 3600) * 1000);    
    this.form.tf_title.value = '';
    this.form.entity.value = 'user';
    this.form.entity_id.value = 23; //TODO : Vrai valeur
    this.form.all_day.value = allDay;
    this.form.action.value = 'quick_insert';
    this.form.date_begin.value = date_begin.format('Y-m-d H:i:s');
    this.form.duration.value = 3600;
    this.form.context.value = context;
    this.date.innerHTML = date_begin.format('Y/m/d H:i') + '-' + date_end.format('Y/m/d H:i');
  },
  
  show: function() {
    this.popup.setStyle('display','block');
  },

  hide: function() {
    this.popup.setStyle('display','none');
  },

  submit: function() {
    eventData = new Object();
    eventData.title = this.form.tf_title.value;
    eventData.entity_id = this.form.entity_id.value;
    eventData.entity = this.form.entity.value ;
    eventData.all_day = this.form.all_day.value;
    eventData.date_begin = this.form.date_begin.value;
    eventData.duration = this.form.duration.value;
    eventData.action = this.form.action.value;
    eventData.context = this.form.context.value;
    eventData.calendar_id = this.form.event_id.value;
    eventData.element_id = this.form.element_id.value;
    eventData.ajax = 1;
    
    if(eventData.action == 'quick_insert') {
      obm.calendarManager.sendCreateEvent(eventData);
    } else {
      obm.calendarManager.sendUpdateEvent(eventData);
    }
    this.hide();
  }
})
