import '@testing-library/jest-dom';

// jsdom has the <dialog> element but does not implement opening and closing it.
HTMLDialogElement.prototype.showModal = function showModal() {
  this.setAttribute('open', '');
};
HTMLDialogElement.prototype.close = function close() {
  this.removeAttribute('open');
};

beforeEach(() => {
  localStorage.clear();
});
