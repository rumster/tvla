// Normalized version

if (root == NULL) {
  root = el;
  return;
}

cur = root;
while (true) {
  if (el->data < cur->data) {
    t = cur->left;
    if (t == NULL) {
      cur->left = el;
      return;
    }
    cur = t;
  } else {
    t = cur->right;
    if (t == NULL) {
      cur->right = el;
      return;
    }
    cur = t;
  }
}


// Non-normalized version

if (root == NULL) {
  root = el;
  return;
}

cur = root;
while (true) {
  if (el->data < cur->data) {
    if (cur->left == NULL) {
      cur->left = el;
      return;
    }
    cur = cur->left;
  } else {
    if (cur->right == NULL) {
      cur->right = el;
      return;
    }
    cur = cur->right;
  }
}
