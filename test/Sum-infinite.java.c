// This is automatically generated by the Tiger compiler.
// Do NOT modify!

extern void *prev;
// structures
struct Sum
{
  struct Sum_vtable *vptr;
  int isObjOrArray;
  int length;
  void* forwarding;
};
struct Doit
{
  struct Doit_vtable *vptr;
  int isObjOrArray;
  int length;
  void* forwarding;
};
// vtables structures
struct Sum_vtable
{
  char* Sum_gc_map;
};

struct Doit_vtable
{
  char* Doit_gc_map;
  int (*get)(struct Doit * this);
  int (*doit)(struct Doit * this,int n);
};


//methods decl
int Doit_get(struct Doit * this);
int Doit_doit(struct Doit * this, int n);
// vtables
struct Sum_vtable Sum_vtable_ = 
{
  "",
};

struct Doit_vtable Doit_vtable_ = 
{
  "",
  Doit_get,
  Doit_doit,
};


//GC stack frames
struct Tiger_main_gc_frame
{
    void *prev_;
    char *arguments_gc_map;
    int *arguments_base_address;
    int locals_gc_map;
    struct Doit * x_0;
};

struct Doit_get_gc_frame
{
    void *prev_;
    char *arguments_gc_map;
    int *arguments_base_address;
    int locals_gc_map;
    int*  a;
};

struct Doit_doit_gc_frame
{
    void *prev_;
    char *arguments_gc_map;
    int *arguments_base_address;
    int locals_gc_map;
    int*  a;
    struct Doit * x_1;
};

// memory GC maps
int Tiger_main_locals_gc_map = 1;

char* Doit_get_arguments_gc_map="1";
int Doit_get_locals_gc_map=1;

char* Doit_doit_arguments_gc_map="10";
int Doit_doit_locals_gc_map=2;

// methods
int Doit_get(struct Doit * this)
{
  struct Doit_get_gc_frame frame;
  frame.prev_=prev;
  prev=&frame;
  frame.arguments_gc_map = Doit_get_arguments_gc_map;
  frame.arguments_base_address = &this;
  frame.locals_gc_map = Doit_get_locals_gc_map;
  int t;
  frame.a=0;

  frame.a = (int*)Tiger_new_array(4);

  prev=frame.prev_;
  return t;
}
int Doit_doit(struct Doit * this, int n)
{
  struct Doit_doit_gc_frame frame;
  frame.prev_=prev;
  prev=&frame;
  frame.arguments_gc_map = Doit_doit_arguments_gc_map;
  frame.arguments_base_address = &this;
  frame.locals_gc_map = Doit_doit_locals_gc_map;
  int sum;
  int i;
  frame.a=0;
  int s;
  frame.x_1=0;

  i = 0;
  sum = 0;
  s = (frame.x_1=this, frame.x_1->vptr->get(frame.x_1));
  frame.a = (int*)Tiger_new_array(4);
  while (i < n)
    {
      i = i + 1;
      sum = sum + 1;
    }
  
  prev=frame.prev_;
  return sum;
}

// main method
int Tiger_main ()
{
    struct Tiger_main_gc_frame frame;
    frame.prev_=prev;
    prev=&frame;
    frame.arguments_gc_map = 0;
    frame.arguments_base_address = 0;
    frame.locals_gc_map = Tiger_main_locals_gc_map;
    frame.x_0=0;
    System_out_println ((frame.x_0=((struct Doit*)(Tiger_new (&Doit_vtable_, sizeof(struct Doit)))), frame.x_0->vptr->doit(frame.x_0, 101)));
}



