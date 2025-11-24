import {MVPEvent} from "./events.js";
import {switchToSubdomain} from './util.js';

export class Broker{
	static instance;
	
	constructor(){
		if (Broker.instance) {
			return Broker.instance;
		}
		Broker.instance = this;

		this.topics = {}; // topics est un Map (dictionnaire); clé: topic ; et valeur: tableau de callback
	}
	
	subscribe(topic,callback){
		if(topic && ! this.topics[topic]){
			this.topics[topic]=[]; // Initialisation à un tableau vide
		}
		this.topics[topic].push(callback);
	}
	
	unsubscribe(topic,callback){
		if (topic && !this.topics[topic]) {
			return;
        }

		// Remove the given callback from the callback list associated with this topic		
		this.topics[topic] = this.topics[topic].filter(function (x) { return x != callback; });
        
		// If this callback is the last one in the callback list, delete the callback list		
		if (!this.topics[topic].length) {
			delete this.topics[topic];
        }		
	}
	
	publish(topic,msg){
		let v = this.topics[topic];
		if(v != null && typeof v !== "undefined"){
			for(var i=0;i<v.length;++i){
				var callback = v[i];
				callback(msg);
			}
		}
	}
}

// This class implements the Subject class in the Observer Design Pattern
export class Observable{
	static broker = new Broker();
	
	constructor(_topic,_value){
		this._topic = _topic;
		this._value = _value;
	}
	
	// This method implements Subject --> notifyObservers()
	notifyObservers(){
		Observable.broker.publish(this._topic,this._value);
	}
	
	//This method implements Subject --> registerObserver(observer)
	register(observer){
		Observable.broker.subscribe(this._topic,observer);
	}

	//This method implements Subject --> unregisterObserver(observer)	
	unregister(observer){
		Observable.broker.unsubscribe(this._topic,observer);
	}
	
	get topic(){
		return this._topic;
	}
	
	get value(){
		return this._value;
	}
	
	set value(val){
		if(val !== this._value){
			this._value = val;
			this.notifyObservers();
		}
	}
}

export class ComputedObservable extends Observable{
	constructor(topic,value,deps){
		super(topic,value());
		const observer = ()=>{
			this._value = value();
			this.notifyObservers();
		};
		
		deps.forEach(dep => dep.register(observer));
	}
	
	get value(){
		return this._value;
	}
	
	set value(_){
		throw 'Cannot set value of a computed observable!';
	}
}

export class Model extends Observable{
	constructor(name){
		super(name,{});
		this.mvpEvent = null;
	}
	
	fireStateChangeEvent(state,evt){
		this.mvpEvent = new MVPEvent(evt);
		this.value = state;
	}
	
	callAPI(_method,_headers,_body,_subdomain,_path,_callback){
		let options = {
			mode: 'cors',
			credentials: 'omit',
			cache: 'no-cache'
		};
		
		if(_method !== undefined && _method !== null && _method !== 'GET'){
			options['method'] = _method; 
		}
		
		if(_headers !== undefined && _headers !== null){
			options['headers'] = _headers;
		}
		
		if(_body !== undefined && _body !== null){
			options['body'] = _body;
		}
				
		fetch(switchToSubdomain(_subdomain,_path),options)
			.then(response => response.json())
			.then(_callback).catch(err=>console.log(err));
	}
	
	loadModel(){
		throw new Error('You have to implement the method loadModel()!');
	}
} //End of class Model

export class View extends Observable{
	constructor(name){
		super(name,{});
		this.mvpEvent = null;
		this.bindings = {};
	}
	
	fireIntentEvent(state,evt){
		this.mvpEvent = new MVPEvent(evt);
		this.value = state;
	}
	
	init(model){
		this.defineBindings(model);
		this.applyBindings();
	}
	
	applyBindings(){
		try{
			document.querySelectorAll("[data-bind]").forEach(elem => {
				const obs = this.bindings[elem.getAttribute("data-bind")];
				this.bindValue(elem,obs);
			});
		}catch(e){
			console.log('Warning Partial Binding!' + e);
		}
	}

	bindValue(input,observable){
		try{
			if(typeof observable.value === 'undefined'){
				observable.value = 'N/A';
			}
			input.value = observable.value;
			observable.register(() => input.value = observable.value);
			input.onkeyup = () => observable.value = input.value;
		}catch(e){
			console.log(`warning: unavailable binding for data-bind: ${input.getAttribute('data-bind')}`);
		}
	}
	
	defineBindings(model){
		throw new Error('You have to implement the method defineBindings(model)!');
	}
	
	toJson(){
		throw new Error('You have to implement the method toJson()!');		
	}
}

export class Presenter{
	constructor(_view,_model){
		this._view = _view;
		this._model = _model;
	}
	get view(){
		return this._view;
	}
	
	get model(){
		return this._model;
	}
	
	load(){
		this._model.loadModel();
	}
}
