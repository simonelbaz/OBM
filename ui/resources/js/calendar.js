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

  
  initialize: function(el,id,uid,time,duration,title,options) {;
    this.setOptions(options);
    this.element = $(el);
    this.id = id;
    this.uid = uid
    this.title = title;
    this.time = time;
    this.size = 1;
    this.head = $ES('h1',this.element);
    if(this.options.draggable)
      this.makeDraggable();
    
    this.setTime(time);
    this.setDuration(duration);
  },

  makeDraggable: function() {
    var dragOptions = {
      handle: $('moveHandler-' + this.id + '-' + this.uid + '-' + this.time),
      xMin: this.options.context.left,
      xMax: this.options.context.right - obm.calendarManager.defaultWidth,
      yMin: this.options.context.top,
      yMax: this.options.context.bottom - this.element.offsetHeight,

      onStart:function() {
        this.element.setStyle('width', obm.calendarManager.defaultWidth + 'px');
        this.element.setStyle('margin-left', '0');
        this.element.setOpacity(.7);
      },

      onComplete:function() {
        this.element.setOpacity(1);
        obm.calendarManager.moveEventTo(this.element.id,this.element.getLeft(),this.element.getTop());
      }     
    };   

    this.drag = this.element.makeDraggable(dragOptions);
  },

  setTime: function(time) {
    origin = (time - obm.calendarManager.startTime) - (time - obm.calendarManager.startTime)%this.options.xUnit;
    if(this.setOrigin(origin)) {
      if(this.time) {
        time = new Date(time * 1000);
        d = new Date(this.time * 1000);
        time.setHours(d.format('H'));
        time.setMinutes(d.format('i'));
        time = time.getTime()/1000;
      };
      this.time = time;
    } else {
      this.redraw();
    }

  },

  setOrigin: function(origin) {
    if(origin < 0) {   
      this.hiddenSize = origin / this.options.xUnit;
      console.log(this.hiddenSize);
      origin = 0;
    } else if(this.hidden != 0) {
        this.hidden = 0;
        this.setSize(this.size);
    }
    hr = $(this.options.type+'-'+origin);
    this.origin = origin;
    head = $('calendarHead');
    if(this.origin >= 0 && head) {
      tr = $(this.options.type+'-'+this.origin).parentNode;
      tr.setStyle('height', (tr.offsetHeight - this.element.offsetHeight) + 'px');
    }        
    if(head) {
       hr.parentNode.setStyle('height', (hr.parentNode.offsetHeight + this.element.offsetHeight) + 'px');
    }
    this.redraw();
    if(obm.calendarManager.lock()) {
      obm.calendarManager.resizeWindow();
      obm.calendarManager.unlock()
    }    
    return true;
  },

  setDuration: function(duration) {
    this.duration = duration;
    date_begin = new Date(this.time * 1000);
    date_end = new Date((this.time + this.duration) * 1000);
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

  conflict: function(nb, position) {
    this.element.setStyle('margin-top',position * this.element.offsetHeight + 'px');
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

  initialize: function(el,id,uid,time,duration,title,options) {
    this.setOptions(options);
    this.element = $(el);
    this.id = id;
    this.uid = uid;
    this.time = time;
    this.title = title;   
    if(this.options.resizable) 
      this.makeResizable(options);
    if(this.options.draggable)
      this.makeDraggable();      
    this.setTime(time);
    this.setDuration(duration);          
  },
  
  setDuration: function(duration) {
    this.duration = duration;
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
      handle: $('resizeHandler-' + this.id + '-' + this.uid + '-' + this.time),
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
      this.time = time;
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

  conflict: function(nb, position) {
    if(resize[i].unit.size > 1)
      evt.setWidth(obm.calendarManager.defaultWidth/resize[i].unit.size);
    else
      evt.setWidth(obm.calendarManager.defaultWidth);
    evt.setMargin((obm.calendarManager.defaultWidth/resize[i].unit.size)*resize[i].position);
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

  newDayEvent: function(id,uid,time,duration,title,options) {
    elId = 'event-'+id+'-'+uid+'-'+time;
    obmEvent = new Obm.CalendarDayEvent(elId,id,uid,time,duration,title,options);
    this.events.put(elId,obmEvent);
    this.register(elId);
  },

  newEvent: function(id,uid,time,duration,title,options) {
    elId = 'event-'+id+'-'+uid+'-'+time;
    obmEvent = new Obm.CalendarEvent(elId,id,uid,time,duration,title,options);
    this.events.put(elId,obmEvent);
    this.register(elId);
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
    diff = event1.time - event2.time;
    if(diff != 0)
      return diff;
    diff = event1.id - event2.id;
    if(diff != 0)
      return diff;
    return event1.uid - event2.uid;
  },

  compareTime: function(time1, time2) {
    return time1.toInt() - time2.toInt();
  },

  moveEventTo: function(id,left,top) {
    evt = this.events.get(id);
    xDelta = Math.round((left-evt.options.context.left)/this.defaultWidth);
    yDelta = Math.floor((top-evt.options.context.top)/this.defaultHeight);
    time = this.startTime + xDelta*evt.options.xUnit + yDelta*evt.options.yUnit;
    if(evt.time != time) {
      this.unregister(id);
      evt.setTime(time);
      this.register(id);    
      this.serverStore(id);
      this.redrawAllEvents(evt.time);
    } else {
      evt.redraw();
    }
  },

  resizeEventTo: function(id,size) {
    size = Math.round(size/this.defaultHeight);
    evt = this.events.get(id);
    if(size != evt.size) {
      this.unregister(id);
      evt.setDuration(size*evt.options.yUnit);
      this.register(id);   
      this.serverStore(id);
      this.redrawAllEvents(evt.time);
    }
  },

  register: function(id) {
    evt = this.events.get(id);
    size = evt.size;
    for(i=0;i < size;i++) {
      t = evt.origin + i*evt.options.unit;
      if(!this.times.get(t)) {
        this.times.put(t.toInt(),new Array());
      }
      this.times.get(t).push(evt);
    }   
  },

  unregister: function(id) {
    size = evt.size;
    for(i=0;i < size;i++) {
      t = evt.origin + i*evt.options.unit;
      this.times.get(t).remove(evt);
    }
  },

  redrawAllEvents: function() {
    keys = this.times.keys().sort(this.compareTime);
    this.redrawEvents(keys);
  }, 

  redrawDayEvents: function(dayStart) {
    //TODO Ca doit pouvoir s'ameliorer

    date = new Date();
    date.setTime(dayStart * 1000);
    date.setHours(8);
    date.setMinutes(0);
    date.setSeconds(0);
    ts = date.getTime()/1000;
    date.setHours(20)
    te = date.getTime()/1000;

    keys = this.times.keys().sort();
    while(keys[0] < ts) {
      keys.shift();
    }
    k = 0;
    while(keys[(keys.length -1)] > te) {
      keys.pop();
    }
    this.redrawEvents(keys);
  },

  redrawEvents: function(keys) {
    resize = new Object();
    end = 0;
    for(k=0;k < keys.length; k++) {
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
      for(i=0;i<time.length;i++) {
        free.push(i);
      }
      // PERFORMING Event position
      for(i=0;i<time.length;i++) {
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
    for(i in resize) {
      evt = this.events.get(i);
      evt.conflict(resize[i].unit.size,resize[i].position);
    }
  },
  
  serverStore: function(id) {
    evt = this.events.get(id);
    date_begin = new Date(evt.time * 1000);
    date_end = new Date(evt.time * 1000 + evt.duration * 1000);
    ajax = new Ajax('calendar_index.php',
    {postBody:'ajax=true&action=quick_update&calendar_id='+evt.id+'&date_begin='+date_begin.format('Y-m-d H:i:s')+'&date_end='+date_end.format('Y-m-d H:i:s')+'&title='+evt.title, onComplete: this.serverComplete,update:'ajaxMessage',method: 'post'});
    ajax.request();
  },

  serverComplete: function() {
    setTimeout(function () {$('ajaxMessage').innerHTML = ''}, 2000);
    ;
  }
});

