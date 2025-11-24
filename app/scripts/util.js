export function switchToSubdomain(subdomain,path){
	return `${window.location.protocol}//${window.location.host.replace(/^([^.])*/,subdomain)}/${path}`;
}

export function switchToWssSubdomain(subdomain,path){
	return `wss://${window.location.host.replace(/^([^.])*/,subdomain)}/${path}`;
}

export function toggleFormElements(formId,bDisabled) {
	let f = document.getElementById(formId);
    let inputs = f.getElementsByTagName("input"); 
    for (var i = 0; i < inputs.length; i++) { 
        inputs[i].disabled = bDisabled;
    } 
    let selects = f.getElementsByTagName("select");
    for (var i = 0; i < selects.length; i++) {
        selects[i].disabled = bDisabled;
    }
    let textareas = f.getElementsByTagName("textarea"); 
    for (var i = 0; i < textareas.length; i++) { 
        textareas[i].disabled = bDisabled;
    }
    let buttons = f.getElementsByTagName("button");
    for (var i = 0; i < buttons.length; i++) {
        buttons[i].disabled = bDisabled;
    }
}

export function validateForm(){
  'use strict'
  const forms = document.querySelectorAll('.needs-validation')
  let modeButton = document.getElementById('modeButton');
  Array.from(forms).forEach(form => {
	modeButton.addEventListener('click', event => {
	  if (!form.checkValidity()) {
		event.preventDefault();
		event.stopImmediatePropagation();
	  }

	  form.classList.add('was-validated')
	}, false)
  })
}

export function getSiblings(elem) {	
	let siblings = [];
	if(!elem.parentNode) {
		return siblings;
	}	
	let sibling = elem.parentNode.firstChild;
	while (sibling) {
		if (sibling.nodeType === 1 && sibling !== elem) {
			siblings.push(sibling);
		}
		sibling = sibling.nextSibling
	}
	return siblings;
}   

export class InlineLocalizer {
	constructor(){
		this.style = document.createElement('style');
		this.style.innerHTML = ".unselect-language{display:none;}";
		document.getElementById('select-language').addEventListener(e=>this.changeLanguage());
		try{
			this.setLanguage(this.resolveLanguage());
		}catch(e){
			console.log(e);
		}
	}
	
	setLanguage(currentLanguage){
		let localizedTags = document.querySelectorAll('[data-lang]');
		localizedTags.forEach(tag => {
			if(!tag.classList.contains('unselect-language')){
				tag.classList.add('unselect-language')
			}
		});
		let currentLocalizedTags = document.querySelectorAll(`[data-lang="${currentLanguage}"]`);
		currentLocalizedTags.forEach(tag => {
			if(tag.classList.contains('unselect-language')){
				tag.classList.remove('unselect-language')
			}
		});
		document.getElementById('select-language').value = currentLanguage;
	}
	
	changeLanguage(){
		let selectLanguage = document.getElementById('select-language');
		let selectedLanguage = selectLanguage.options[selectLanguage.selectedIndex].value;
		sessionStorage.setItem('lang',selectedLanguage);
		this.setLanguage(selectedLanguage);
	}
	
	resolveLanguage(){
		let language = window.navigator.userLanguage || window.navigator.language;
		let lang = language.substr(0,2).toLowerCase();
		let sessionLang = null;
		try{ sessionLang = sessionStorage.getItem('lang'); }catch(e){}
		if(sessionLang !== null) lang = sessionLang;
		if(document.querySelectorAll(`[data-lang="${lang}"]`).length === 0){
			lang='en';
		}
		return lang; 
	}
}

export class PWALocalizer {
	constructor(messages){
		this.messages = messages;
	}
	
	static async init(){
		let language = window.navigator.userLanguage || window.navigator.language;
		let lang = language.substr(0,2).toLowerCase();
		let sessionLang = null;
		try{ sessionLang = sessionStorage.getItem('lang'); }catch(e){}
		if(sessionLang !== null) lang = sessionLang;
		let response = await fetch(`/_locales/${lang}/messages.json`);
		if(!response.ok){
			response = await fetch(`/_locales/en/messages.json`);
		}
		const messages = await response.json();
		return new PWALocalizer(messages);
	}
	
	setLanguage(currentLanguage){
		document.querySelectorAll('[data-i18n]').forEach((el) => {
			const i18nKey = el.getAttribute('data-i18n');
			try{
				let placeholders = JSON.parse(el.getAttribute('data-i18n-placeholders'));
				if (!Array.isArray(placeholders)) {
					el.innerText = this.messages[i18nKey].message.replace(/\$\w+\$/g, function(placeholder) {
						return placeholder in placeholders ? placeholders[placeholder] : placeholder;
					});
					return;
				}
				let actualPlaceholders = {};
				Object.keys(this.messages[i18nKey].placeholders).forEach((key)=>{
					actualPlaceholders[`$${key.toUpperCase()}$`] = placeholders[Number(this.messages[i18nKey].placeholders[key].content.substring(1))];
				});
				el.innerText = this.messages[i18nKey].message.replace(/\$\w+\$/g, function(placeholder) {
					return placeholder in actualPlaceholders ? actualPlaceholders[placeholder] : placeholder;
				});
				return;
			}catch(e){}
			el.innerText = this.messages[i18nKey].message;
		});
	}
	
	resolveLanguage(){
		let language = window.navigator.userLanguage || window.navigator.language;
		let lang = language.substr(0,2).toLowerCase();
		let sessionLang = null;
		try{ sessionLang = sessionStorage.getItem('lang'); }catch(e){}
		if(sessionLang !== null) lang = sessionLang;
		return lang; 
	}
}
