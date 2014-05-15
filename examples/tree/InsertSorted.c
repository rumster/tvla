cur = root;
while (cur != NULL && cur->data != el->data) {
  prev = cur;
  if (el->data < cur->data)
    cur = cur->left;
  else cur = cur->right;
}

// Don't insert duplicates
if (cur == NULL) {
  if (cur == root) {
    root = el;
    return;
  }
  if (el->data < prev->data)
    prev->left = el;
  else prev->right = el;
}
