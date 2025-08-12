
if(!document.queryCommandSupported) {
    document.queryCommandSupported = () => false
}



window.confirm = (message, title, doYes) => true


function noOp () { }
if (typeof window.URL.createObjectURL === 'undefined') {
    Object.defineProperty(window.URL, 'createObjectURL', { value: noOp})
}


// if (typeof this.global.TextEncoder === 'undefined') {
//     const { TextEncoder } = require('util');
//     this.global.TextEncoder = TextEncoder;
// }